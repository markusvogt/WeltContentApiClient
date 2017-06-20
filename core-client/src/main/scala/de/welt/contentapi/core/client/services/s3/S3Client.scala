package de.welt.contentapi.core.client.services.s3

import java.time.Instant
import javax.inject.{Inject, Singleton}

import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.services.s3.AmazonS3Client
import com.amazonaws.services.s3.model._
import com.amazonaws.util.StringInputStream
import de.welt.contentapi.utils.Loggable
import play.api.{Configuration, Environment}

import scala.io.{Codec, Source}

sealed trait S3Client extends Loggable {

  private val EndpointConfigKey = "welt.aws.s3.endpoint"

  val config: Configuration
  val environment: Environment
  implicit val codec: Codec = Codec.UTF8

  val client: AmazonS3Client = {
    val endpoint = config.get[String](EndpointConfigKey)
    val clazz = classOf[Environment]
    println("Environment jar" + clazz.getResource("/" + clazz.getName.replace('.', '/') + ".class"))
    val s3Client = environment.mode match {
      case play.api.Mode.Prod ⇒ new AmazonS3Client()
      case _ ⇒ new AmazonS3Client(
        new BasicAWSCredentials(
          config.get[String]("welt.aws.s3.dev.accessKey"),
          config.get[String]("welt.aws.s3.dev.secretKey")
        )
      )
    }
    s3Client.setEndpoint(endpoint)
    log.debug(s"s3 connected to $endpoint")
    s3Client
  }

  def get(bucket: String, key: String): Option[String] = withS3Result(bucket, key)({
    log.debug(s"get($key)")
    result ⇒ Source.fromInputStream(result.getObjectContent).mkString
  })

  def getWithLastModified(bucket: String, key: String): Option[(String, Instant)] = withS3Result(bucket, key)({
    result ⇒
      val content = Source.fromInputStream(result.getObjectContent).mkString
      val lastModified = result.getObjectMetadata.getLastModified.toInstant
      (content, lastModified)
  })

  def getLastModified(bucket: String, key: String): Option[Instant] = withS3Result(bucket, key)({
    result ⇒ result.getObjectMetadata.getLastModified.toInstant
  })

  def putPrivate(bucket: String, key: String, value: String, contentType: String): Unit = {
    //    log.info(s"put($key, $value, $contentType)")
    put(bucket, key, value, contentType)
  }

  private def withS3Result[T](bucket: String, key: String)(action: S3Object ⇒ T): Option[T] =
    try {
      val request = new GetObjectRequest(bucket, key)
      val result = client.getObject(request)

      // http://stackoverflow.com/questions/17782937/connectionpooltimeoutexception-when-iterating-objects-in-s3
      try {
        Some(action(result))
      } finally {
        result.close()
      }
    } catch {
      case e: AmazonS3Exception if e.getStatusCode == 404 ⇒
        log.debug("not found at %s - %s" format(bucket, key))
        None
    }


  private def put(bucket: String, key: String, value: String, contentType: String) {
    val metadata = new ObjectMetadata()
    metadata.setContentType(contentType)
    metadata.setContentLength(value.getBytes("UTF-8").length)

    val request = new PutObjectRequest(bucket, key, new StringInputStream(value), metadata)
      .withCannedAcl(CannedAccessControlList.Private)


    client.putObject(request)
  }
}

@Singleton
class S3ClientImpl @Inject()(override val config: Configuration, override val environment: Environment) extends S3Client {}
