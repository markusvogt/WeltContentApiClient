package de.welt.contentapi.raw.client.services

import de.welt.contentapi.core.client.services.s3.S3Client
import de.welt.contentapi.utils.Env
import de.welt.testing.DisabledCache
import org.mockito.{Matchers, Mockito}
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.{Configuration, Environment, Mode}

class RawTreeServiceTest extends PlaySpec with MockitoSugar {

  "Configuration" must {

    val expectedBucket = "foo-bucket"
    val expectedFolder = "foo-folder"
    val expectedMode = "foo-mode"
    val expectedPlayMode = "prod"


    sealed trait ConfigurationScope {
      val environment: Environment = mock[Environment]
      val s3Client: S3Client = mock[S3Client]

      Mockito.when(environment.mode) thenReturn Mode.Prod
      Mockito.when(s3Client.get(Matchers.anyString(), Matchers.anyString())) thenReturn None
    }

    "use 'mode' from config file" in new ConfigurationScope {
      // given
      val config = Configuration(
        RawTreeServiceImpl.bucketConfigKey → expectedBucket,
        RawTreeServiceImpl.folderConfigKey → expectedFolder,
        RawTreeServiceImpl.modeConfigKey → expectedMode
      )
      val service = new RawTreeServiceImpl(s3Client, config = config, environment = environment, DisabledCache)

      // when
      service.root(env = Env.Live)

      // then
      Mockito.verify(s3Client).get(expectedBucket, s"$expectedFolder/$expectedMode/Live/config.json")
    }

    "use 'play.Mode' as fallback" in new ConfigurationScope {
      // given
      val config = Configuration(
        RawTreeServiceImpl.bucketConfigKey → expectedBucket,
        RawTreeServiceImpl.folderConfigKey → expectedFolder
      )
      val service = new RawTreeServiceImpl(s3Client, config = config, environment = environment, DisabledCache)

      // when
      service.root(env = Env.Live)

      // then
      Mockito.verify(s3Client).get(expectedBucket, s"$expectedFolder/$expectedPlayMode/Live/config.json")
    }

  }

}
