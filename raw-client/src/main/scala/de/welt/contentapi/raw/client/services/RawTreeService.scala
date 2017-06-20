package de.welt.contentapi.raw.client.services

import javax.inject.{Inject, Singleton}

import de.welt.contentapi.core.client.services.s3.S3Client
import de.welt.contentapi.raw.models.RawChannel
import de.welt.contentapi.utils.Env.Env
import de.welt.contentapi.utils.Loggable
import play.api.cache.SyncCacheApi
import play.api.libs.json.{JsError, JsSuccess, Json}
import play.api.{Configuration, Environment, Mode}

import scala.concurrent.duration._

trait RawTreeService {
  def root(implicit env: Env): Option[RawChannel]
}

@Singleton
class RawTreeServiceImpl @Inject()(s3Client: S3Client,
                                   config: Configuration,
                                   environment: Environment,
                                   cache: SyncCacheApi) extends RawTreeService with Loggable {

  import RawTreeServiceImpl._

  /**
    * S3 bucket name
    */
  val bucket: String = config.get[String](bucketConfigKey)

  /**
    * S3 root folder for the raw tree
    */
  val folder: String = config.get[String](folderConfigKey)

  /**
    * prod/dev/local-dev mode
    * This is only a sub folder with the Live/Preview raw tree
    */
  private val mode: String = if (config.has(modeConfigKey)) {
    config.get[String](modeConfigKey)
  } else {
    // playMode is a fallback for api-client-version >0.6.x
    environment.mode match {
      case Mode.Prod ⇒ "prod"
      case _ ⇒ "dev"
    }
  }

  protected def objectKeyForEnv(env: Env): String = s"$folder/$mode/${env.toString}/config.json"

  // todo (mana): add metrics
  def root(implicit env: Env): Option[RawChannel] = {
    cache.getOrElseUpdate(s"rawChannelData-$env", 1.minutes) {
      s3Client.get(bucket, objectKeyForEnv(env)).flatMap { tree ⇒
        Json.parse(tree).validate[RawChannel](de.welt.contentapi.raw.models.RawReads.rawChannelReads) match {
          case JsSuccess(root, _) ⇒
            log.info(s"Loaded/Refreshed raw tree for $env")
            root.updateParentRelations()
            Some(root)
          case e: JsError ⇒
            log.error(f"JsError parsing S3 file: '$bucket/$folder'. " + JsError.toJson(e).toString())
            None
        }
      }
    }
  }
}

object RawTreeServiceImpl {
  val bucketConfigKey = "welt.aws.s3.rawTree.bucket"
  val folderConfigKey = "welt.aws.s3.rawTree.folder"
  val modeConfigKey = "welt.aws.s3.rawTree.mode"
}
