package de.welt.contentapi.pressed.client.converter

import de.welt.contentapi.pressed.models._
import de.welt.contentapi.raw.models._
import de.welt.testing.TestHelper.raw.channel._
import org.scalatest.{FlatSpec, Matchers}

class Raw2ApiConfigurationTests extends FlatSpec with Matchers {

  trait TestScopeConfiguration {
    val rawChannelMetadata = RawChannelMetadata(
      title = Some("Meta Title"),
      description = Some("Meta Description"),
      keywords = Some(Seq("Keyword1", "Keyword2")),
      contentRobots = Some(RawChannelMetaRobotsTag(noFollow = Some(false), noIndex = Some(false))),
      sectionRobots = Some(RawChannelMetaRobotsTag(noFollow = Some(true), noIndex = Some(true)))
    )
    val rawChannelTheme: RawChannelTheme = RawChannelTheme(
      name = Some("theme"),
      fields = Some(Map("key1" -> "value2", "key2" -> "value2"))
    )
    val rawChannelConfiguration = RawChannelConfiguration(
      metadata = Some(rawChannelMetadata),
      header = Some(
        RawChannelHeader(
          label = Some("label"),
          sectionReferences = Some(Seq(RawSectionReference(Some("Label"), Some("/Path/")))),
          adIndicator = true
        )
      ),
      sponsoring = RawChannelSponsoring(
        logo = Some("logo"),
        slogan = Some("slogan")
      ),
      theme = Some(rawChannelTheme),
      commercial = RawChannelCommercial(definesAdTag = true, definesVideoAdTag = true)
    )
    val node100 = emptyWithIdAndConfig(100, rawChannelConfiguration)
  }

  val converter: RawToApiConverter = new RawToApiConverter(new InheritanceCalculator())

  "ApiMetaConfiguration" must "have 'title', 'tags'(from keywords) and 'description'" in new TestScopeConfiguration {
    val maybeApiMetaConfiguration: Option[ApiMetaConfiguration] = converter.apiMetaConfigurationFromRawChannel(rawChannel = node100)
    val apiMetaConfiguration = maybeApiMetaConfiguration.getOrElse(throw new RuntimeException("Test failed. Should have been Some."))
    apiMetaConfiguration.title shouldBe rawChannelMetadata.title
    apiMetaConfiguration.tags shouldBe rawChannelMetadata.keywords
    apiMetaConfiguration.description shouldBe rawChannelMetadata.description
  }

  it must "have 'content' and 'section' meta robot tags" in new TestScopeConfiguration {
    val apiMetaConfiguration: ApiMetaConfiguration = converter
      .apiMetaConfigurationFromRawChannel(rawChannel = node100)
      .getOrElse(throw new RuntimeException("Test failed. Should have been Some."))
    apiMetaConfiguration.contentMetaRobots.flatMap(_.noFollow) shouldBe rawChannelMetadata.contentRobots.flatMap(_.noFollow)
    apiMetaConfiguration.contentMetaRobots.map(_.noIndex) shouldBe rawChannelMetadata.contentRobots.map(_.noIndex)

    apiMetaConfiguration.sectionMetaRobots.map(_.noFollow) shouldBe rawChannelMetadata.sectionRobots.map(_.noFollow)
    apiMetaConfiguration.sectionMetaRobots.map(_.noIndex) shouldBe rawChannelMetadata.sectionRobots.map(_.noIndex)
  }

  "ApiMetaRobots" must "have content tags" in new TestScopeConfiguration {
    val apiMetaRobots: ApiMetaRobots = converter.apiMetaRobotsFromRawChannelMetaRobotsTag(rawChannelMetadata.contentRobots.get)
  }

  "ApiCommercialConfiguration" must "have 3rd-Party configuration with default values from Raw Constructor" in new TestScopeConfiguration {
    val apiConfiguration: ApiConfiguration = converter.apiConfigurationFromRawChannel(node100)
    // node has no explicit configuration
    val commercialConfiguration: ApiCommercialConfiguration = apiConfiguration.commercial.getOrElse(throw new RuntimeException("Test failed!"))
    val apiThirdParty: ApiCommercial3rdPartyConfiguration = commercialConfiguration.thirdParty.getOrElse(throw new RuntimeException("Test failed!"))
    val apiTaboola: ApiCommercialTaboolaConfiguration = apiThirdParty.contentTaboola.getOrElse(throw new RuntimeException("Test failed!"))
    val defaultsFromRaw = RawChannelCommercial()

    commercialConfiguration.adIndicator shouldBe Some(true)
    apiTaboola.showNews shouldBe Some(defaultsFromRaw.contentTaboola.showNews)
    apiTaboola.showWeb shouldBe Some(defaultsFromRaw.contentTaboola.showWeb)
    apiTaboola.showNetwork shouldBe Some(defaultsFromRaw.contentTaboola.showNetwork)
  }

  // this is a high level test - expected values are tested above
  "ApiConfiguration" must "have 'commercial', 'theme', 'header', 'metadata' and 'sponsoring'" in new TestScopeConfiguration {
    val apiConfiguration: ApiConfiguration = converter.apiConfigurationFromRawChannel(node100)
    apiConfiguration.commercial.flatMap(_.pathForAdTag).isDefined shouldBe true
    apiConfiguration.theme shouldBe None
    apiConfiguration.header.flatMap(_.label).isDefined shouldBe true
    apiConfiguration.meta.flatMap(_.title).isDefined shouldBe true
    apiConfiguration.sponsoring.flatMap(_.name).isDefined shouldBe true
  }

  "ApiSponsoringConfiguration" must "have 'logo', 'slogan' and 'hidden'" in new TestScopeConfiguration {
    private val apiSponsoringConfiguration: ApiSponsoringConfiguration = converter.apiSponsoringConfigurationFromRawChannel(node100)

    apiSponsoringConfiguration.name shouldBe Some("logo") // testing copied deprecated value from `logo`
    apiSponsoringConfiguration.logo shouldBe Some("logo")
    apiSponsoringConfiguration.slogan shouldBe Some("slogan")
    apiSponsoringConfiguration.hidden shouldBe Some(false)
  }

  "ApiTheme" must "have label and fields from the RawChannelTheme" in new TestScopeConfiguration {
    val apiThemeConfiguration = converter.apiThemeFromRawChannel(rawChannel = node100).getOrElse(throw new RuntimeException("Test failed!"))
    apiThemeConfiguration.name shouldBe rawChannelTheme.name
    apiThemeConfiguration.unwrappedFields shouldBe rawChannelTheme.unwrappedFields
  }

}



