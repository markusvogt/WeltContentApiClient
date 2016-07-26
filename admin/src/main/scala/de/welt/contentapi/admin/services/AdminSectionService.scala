package de.welt.contentapi.admin.services

import java.time.Instant
import javax.inject.{Inject, Singleton}

import com.google.common.base.Stopwatch
import de.welt.contentapi.client.services.configuration.ContentClientConfig
import de.welt.contentapi.client.services.contentapi.admin.LegacySectionService
import de.welt.contentapi.client.services.contentapi.{SectionService, SectionServiceImpl}
import de.welt.contentapi.client.services.s3.S3
import de.welt.contentapi.core.models._
import de.welt.contentapi.core.models.writes.FullChannelWrites
import de.welt.contentapi.core.traits.Loggable
import play.api.Environment
import play.api.cache.CacheApi
import play.api.libs.json.Json

trait AdminSectionService extends SectionService {

  def updateChannel(channel: Channel, updatedChannelData: ChannelData, user: String)(implicit env: Env): Option[Channel]

  def syncWithLegacy(): Unit

}

@Singleton
class AdminSectionServiceImpl @Inject()(config: ContentClientConfig,
                                        s3: S3,
                                        environment: Environment,
                                        legacySectionService: LegacySectionService,
                                        cache: CacheApi)
  extends SectionServiceImpl(config, cache, s3, environment) with AdminSectionService with Loggable {


  override def updateChannel(channel: Channel, updatedChannelData: ChannelData, user: String)
                            (implicit env: Env): Option[Channel] = {

    // update channel (lastModified), currently adData and fields allowed only
    channel.data = channel.data.copy(adData = updatedChannelData.adData, fields = updatedChannelData.fields)
    channel.lastModifiedDate = Instant.now.toEpochMilli
    channel.metadata = Some(ChannelMetadataNew(user, Instant.now.toEpochMilli))

    log.info(s"$channel changed by $user")

    // save changes
    save

    // reload changes from s3
    val freshRootNode = root
    val updatedChannel = freshRootNode.flatMap(_.findByPath(channel.id.path))
    log.debug(s"Updated Channel from fresh s3 data $updatedChannel")
    updatedChannel
  }

  override def syncWithLegacy(): Unit = {
    def mergeAndSave(updates: Channel, env: Env): Option[ChannelUpdate] = {
      val maybeRoot = root(env)
      val mergeResult = maybeRoot.map(root => ChannelTools.merge(root, updates))
      maybeRoot.foreach(root ⇒ saveChannel(root)(env))
      mergeResult
    }
    log.info(s"[Sync] starting sync. ")
    val stopwatch = Stopwatch.createStarted()
    val updates = legacySectionService.getSectionData.toChannel

    mergeAndSave(updates, Preview)
    val mergeUpdate = mergeAndSave(updates, Live)

    mergeUpdate.foreach(update ⇒ log.info(s"[Sync] Done syncing with legacy, found the following changes: $update"))
    log.info(s"[Sync] took ${stopwatch.stop.toString}")
  }


  override protected[services] def root(implicit env: Env): Option[Channel] = super.root.orElse {
    log.warn("No data found in s3 bucket, creating new data set from scratch.")
    val root = legacySectionService.getSectionData.toChannel

    saveChannel(root)(Preview)
    saveChannel(root)(Live)
    Some(root)
  }

  private def saveChannel(ch: Channel)(implicit env: Env) = {
    import FullChannelWrites._

    ch.applyChannelUpdates()

    val serializedChannelData = Json.toJson(ch).toString

    log.info(s"saving channel tree to s3 -> ${objectKeyForEnv(env)}")

    s3.putPrivate(config.aws.s3.janus.bucket, objectKeyForEnv(env), serializedChannelData, "application/json")

    log.debug("Invalidating cache.")
    cache.remove(env.toString)
  }

  private def save(implicit env: Env) = {
    root.foreach(r ⇒ saveChannel(r))
  }
}

object ChannelTools extends Loggable {

  def diff(current: Channel, update: Channel): ChannelUpdate = {

    if (current != update) {
      log.debug(s"Cannot diff($this, $update, because they are not .equal()")
      ChannelUpdate(Seq.empty, Seq.empty, Seq.empty)
    } else {

      val bothPresentIds = current.children.map(_.id).intersect(update.children.map(_.id))
      val updatesFromChildren = bothPresentIds.flatMap { id ⇒
        val tupleOfMatchingChannels = current.children.find(_.id == id).zip(update.children.find(_.id == id))

        tupleOfMatchingChannels.map { tuple ⇒
          diff(tuple._1, tuple._2)
        }
      }
      // elements that are no longer in `other.children`
      val deletedByOther = current.children.diff(update.children)
      // additional elements from `other.children`
      val addedByOther = update.children.diff(current.children)

      log.debug(s"[$this] added locally: $addedByOther")
      log.debug(s"[$this] deleted locally: $deletedByOther")

      val moved = {
        lazy val currentRoot = current.root

        // if we can find it in our tree, it hasn't been added but only moved
        val notAddedButMoved = addedByOther.filter { elem ⇒ currentRoot.findByEce(elem.id.ece).isDefined }
        log.debug(s"[$this] not added but moved: $notAddedButMoved")

        lazy val otherRoot = update.root
        // if we can find the deleted elem, it has been moved
        val notDeletedButMoved = deletedByOther.filter { elem ⇒ otherRoot.findByEce(elem.id.ece).isDefined }
        log.debug(s"[$this] not deleted but moved: $notDeletedButMoved")

        notAddedButMoved ++ notDeletedButMoved
      }
      log.debug(s"[$this] moved: $moved")

      val deleted = deletedByOther.diff(moved)
      val added = addedByOther.diff(moved)

      log.debug(s"[$this] deleted globally: $deleted")
      log.debug(s"[$this] added globally: $added")

      val channelUpdate = ChannelUpdate(added, deleted, moved).merge(updatesFromChildren)
      log.debug(s"[$this] Changes: $channelUpdate\n\n")
      channelUpdate
    }
  }

  def merge(current: Channel, other: Channel): ChannelUpdate = {

    val channelUpdate = diff(current, other)

    channelUpdate.deleted.foreach { deletion ⇒
      deletion.parent.foreach { parent ⇒
        parent.children = parent.children.diff(Seq(deletion))
      }
    }

    channelUpdate.added.foreach { addition ⇒
      current.children = current.children :+ addition
    }

    channelUpdate.moved.foreach { moved ⇒
      // remove from current parent
      moved.parent.foreach { parent ⇒
        parent.children = parent.children.diff(Seq(moved))
      }
      // add to new parent
      val newParentId = other.findByEce(moved.id.ece)
        .flatMap(_.parent)
        .map(_.id.ece)

      newParentId.foreach { parentId ⇒
        current.root.findByEce(parentId).foreach { newParent ⇒
          newParent.children = newParent.children :+ moved
        }
      }
    }

    // update master data (path and displayName) from legacy source
    updateData(current, other)

    // for logging
    channelUpdate
  }

  /**
    * copies some data from the `legacyRoot` tree to the `current` tree
    * @param current the destination where to write updates to
    * @param legacyRoot the source object where to read updates from
    */
  def updateData(current: Channel, legacyRoot: Channel): Unit = {
    legacyRoot.findByEce(current.id.ece).foreach(other ⇒ current.updateMasterData(other))
    current.children.foreach(child ⇒ updateData(child, legacyRoot))
  }


}



