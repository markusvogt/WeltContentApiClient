package de.welt.testing

import play.api.cache.SyncCacheApi

import scala.concurrent.duration.Duration
import scala.reflect.ClassTag

/** Implementation of `CacheApi` for testing purpose only */
object DisabledCache extends SyncCacheApi {
  override def set(key: String, value: Any, expiration: Duration): Unit = ???

  override def get[T: ClassTag](key: String): Option[T] = ???

  override def getOrElseUpdate[A: ClassTag](key: String, expiration: Duration = Duration.Inf)(orElse: ⇒ A): A = orElse

  override def remove(key: String): Unit = {}
}
