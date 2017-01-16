package de.welt.contentapi.pressed.client.converter

import javax.inject.Inject

import de.welt.contentapi.core.models.ApiReference
import de.welt.contentapi.pressed.models._
import de.welt.contentapi.raw.models.{RawChannel, RawChannelCommercial, RawChannelMetaRobotsTag, RawSectionReference}


class RawToApiConverter @Inject()(inheritanceCalculator: InheritanceCalculator) {
  private val pathForAdTagAction: InheritanceAction[String] = InheritanceAction[String](
    forRoot = c ⇒ "home", // root has a unique adTag
    forFallback = c ⇒ "sonstiges", // fallback value for First-Level-Sections with no own adTag
    forMatching = c ⇒ trimPathForAdTag(c.id.path)
  )

  /**
    * Converter method that takes a rawChannel and returns an ApiChannel from its data
    *
    * @param rawChannel the rawChannel produced by ConfigMcConfigFace
    * @return a new ApiChannel with the data from the rawChannel
    */
  def apiChannelFromRawChannel(rawChannel: RawChannel): ApiChannel = {
    ApiChannel(
      section = Some(getApiSectionReferenceFromRawChannel(rawChannel)),
      breadcrumb = Some(getBreadcrumb(rawChannel)),
      brand = Some(calculateBrand(rawChannel))
    )
  }

  private[converter] def getApiSectionReferenceFromRawChannel(rawChannel: RawChannel): ApiReference = {
    ApiReference(
      label = Some(rawChannel.id.label),
      href = Some(rawChannel.id.path)
    )
  }

  private[converter] def getBreadcrumb(raw: RawChannel): Seq[ApiReference] = raw.getBreadcrumb.map(b ⇒ ApiReference(Some(b.id.label), Some(b.id.path)))

  /**
    * Converter method that takes a rawChannel and returns an ApiConfiguration from its data
    *
    * @param rawChannel the rawChannel produced by ConfigMcConfigface
    * @return a new ApiConfiguration Object with the data from the rawChannel
    */
  def apiConfigurationFromRawChannel(rawChannel: RawChannel) = ApiConfiguration(
    meta = apiMetaConfigurationFromRawChannel(rawChannel),
    commercial = Some(apiCommercialConfigurationFromRawChannel(rawChannel)),
    sponsoring = Some(apiSponsoringConfigurationFromRawChannel(rawChannel)),
    header = Some(apiHeaderConfigurationFromRawChannel(rawChannel)),
    theme = calculateTheme(rawChannel)
  )

  private[converter] def calculatePathForVideoAdTag(rawChannel: RawChannel): String =
    inheritanceCalculator.forChannel[String](rawChannel, pathForAdTagAction, c ⇒ c.config.commercial.definesVideoAdTag)

  private[converter] def calculatePathForAdTag(rawChannel: RawChannel): String =
    inheritanceCalculator.forChannel[String](rawChannel, pathForAdTagAction, c ⇒ c.config.commercial.definesAdTag)

  private[converter] def trimPathForAdTag(path: String): String = {
    val pathWithoutLeadingAndTrailingSlashes = path.stripPrefix("/").stripSuffix("/").trim
    Option(pathWithoutLeadingAndTrailingSlashes).filter(_.nonEmpty).getOrElse("sonstiges")
  }

  private[converter] def calculateBrand(rawChannel: RawChannel): Boolean = {
    val brandInheritanceAction: InheritanceAction[Boolean] = InheritanceAction[Boolean](
      forRoot = c ⇒ false, // root is never a brand
      forFallback = c ⇒ false, // last channel before root with brand == false
      forMatching = c ⇒ true // ignore `c` -- it's always `true`
    )
    inheritanceCalculator.forChannel[Boolean](rawChannel, brandInheritanceAction, c ⇒ c.config.brand)
  }

  private[converter] def calculateTheme(rawChannel: RawChannel): Option[ApiThemeConfiguration] = {
    val maybeThemeMapping: RawChannel ⇒ Option[ApiThemeConfiguration] = c ⇒ c.config.theme.map(t ⇒ ApiThemeConfiguration(t.name, t.fields))
    val themeInheritanceAction: InheritanceAction[Option[ApiThemeConfiguration]] = InheritanceAction[Option[ApiThemeConfiguration]](
      forRoot = c => None, // The Frontpage has never a theme
      forFallback = maybeThemeMapping,
      forMatching = maybeThemeMapping
    )
    inheritanceCalculator.forChannel[Option[ApiThemeConfiguration]](rawChannel, themeInheritanceAction, c ⇒ c.config.theme.exists(_.name.isDefined))
  }

  private[converter] def apiMetaConfigurationFromRawChannel(rawChannel: RawChannel): Option[ApiMetaConfiguration] = {
    rawChannel.config.metadata.map(metadata => ApiMetaConfiguration(
      title = metadata.title,
      description = metadata.description,
      tags = metadata.keywords,
      contentMetaRobots = metadata.contentRobots.map(apiMetaRobotsFromRawChannelMetaRobotsTag),
      sectionMetaRobots = metadata.sectionRobots.map(apiMetaRobotsFromRawChannelMetaRobotsTag)
    ))
  }

  private[converter] def apiMetaRobotsFromRawChannelMetaRobotsTag(rawChannelMetaRobotsTag: RawChannelMetaRobotsTag): ApiMetaRobots =
    ApiMetaRobots(noIndex = rawChannelMetaRobotsTag.noIndex, noFollow = rawChannelMetaRobotsTag.noFollow)

  /**
    * Always calculates adTags, in doubt 'sonstiges' -> not optional
    *
    * @param rawChannel source Channel to take the data from
    * @return a resolved 'ApiCommercialConfiguration' containing a videoAdTag and an adTag
    */
  private[converter] def apiCommercialConfigurationFromRawChannel(rawChannel: RawChannel): ApiCommercialConfiguration = {
    ApiCommercialConfiguration(
      pathForAdTag = Some(calculatePathForAdTag(rawChannel)),
      pathForVideoAdTag = Some(calculatePathForVideoAdTag(rawChannel)),
      thirdParty = Some(thirdPartyCommercialFromRawChannelCommercial(rawChannel.config.commercial)),
      adIndicator = rawChannel.config.header.map(_.adIndicator)
    )
  }

  private[converter] def apiSponsoringConfigurationFromRawChannel(rawChannel: RawChannel): ApiSponsoringConfiguration = {
    ApiSponsoringConfiguration(
      name = rawChannel.config.sponsoring.logo,
      logo = rawChannel.config.sponsoring.logo,
      slogan = rawChannel.config.sponsoring.slogan,
      hidden = Some(rawChannel.config.sponsoring.hidden)
    )
  }

  private[converter] def apiHeaderConfigurationFromRawChannel(rawChannel: RawChannel) = {
    val apiSectionReferences: Seq[ApiReference] = mapReferences(
      rawChannel.config.header.map(_.unwrappedSectionReferences).getOrElse(Nil)
    )
    ApiHeaderConfiguration(
      label = rawChannel.config.header.flatMap(_.label),
      logo = rawChannel.config.header.flatMap(_.logo),
      slogan = rawChannel.config.header.flatMap(_.slogan),
      sectionReferences = Some(apiSectionReferences)
    )
  }
  /**
    * Simple Raw Reference -> Api Reference Converter
    */
  def mapReferences(references: Seq[RawSectionReference]): Seq[ApiReference] = {
    references.map(ref ⇒ ApiReference(ref.label, ref.path))
  }

  private[converter] def apiThemeFromRawChannel(rawChannel: RawChannel): Option[ApiThemeConfiguration] =
    rawChannel.config.theme.map(t ⇒ ApiThemeConfiguration(t.name, t.fields))

  private[converter] def thirdPartyCommercialFromRawChannelCommercial(rawChannelCommercial: RawChannelCommercial) =
    ApiCommercial3rdPartyConfiguration(
      contentTaboola = Some(ApiCommercialTaboolaConfiguration(
        showNews = Some(rawChannelCommercial.contentTaboola.showNews),
        showWeb = Some(rawChannelCommercial.contentTaboola.showWeb),
        showNetwork = Some(rawChannelCommercial.contentTaboola.showNetwork)
      ))
    )
}
