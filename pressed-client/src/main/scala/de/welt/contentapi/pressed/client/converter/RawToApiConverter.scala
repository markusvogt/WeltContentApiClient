package de.welt.contentapi.pressed.client.converter

import javax.inject.Inject

import de.welt.contentapi.core.models.ApiReference
import de.welt.contentapi.pressed.models._
import de.welt.contentapi.raw.models.{RawChannel, RawChannelCommercial, RawChannelMetaRobotsTag, RawSectionReference}


class RawToApiConverter @Inject()(inheritanceCalculator: InheritanceCalculator) {
  private val pathForAdTagAction: InheritanceAction[String] = InheritanceAction[String](
    forRoot = _ ⇒ "home", // root has a unique adTag
    forFallback = _ ⇒ "sonstiges", // fallback value for First-Level-Sections with no own adTag
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
      master = calculateMaster(rawChannel),
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

  private[converter] def calculateMaster(rawChannel: RawChannel): Option[ApiReference] = {
    val rawChannelToApiReference: RawChannel ⇒ Option[ApiReference] = c ⇒ Some(ApiReference(label = Some(c.id.label), href = Some(c.id.path)))
    val masterChannelInheritanceAction: InheritanceAction[Option[ApiReference]] = InheritanceAction[Option[ApiReference]](
      forRoot = c ⇒ rawChannelToApiReference.apply(c.root),
      forFallback = _ ⇒ None,
      forMatching = rawChannelToApiReference
    )
    val predicate: RawChannel ⇒ Boolean = c ⇒ c.parent.contains(c.root) || c.config.master
    inheritanceCalculator.forChannel[Option[ApiReference]](rawChannel, masterChannelInheritanceAction, predicate)
  }

  private[converter] def calculateBrand(rawChannel: RawChannel): Boolean = {
    val brandInheritanceAction: InheritanceAction[Boolean] = InheritanceAction[Boolean](
      forRoot = _ ⇒ false, // root is never a brand
      forFallback = _ ⇒ false, // last channel before root with brand == false
      forMatching = _ ⇒ true // ignore `c` -- it's always `true`
    )
    inheritanceCalculator.forChannel[Boolean](rawChannel, brandInheritanceAction, c ⇒ c.config.brand)
  }

  private[converter] def calculateTheme(rawChannel: RawChannel): Option[ApiThemeConfiguration] = {
    val maybeThemeMapping: RawChannel ⇒ Option[ApiThemeConfiguration] = c ⇒ c.config.theme.map(t ⇒ ApiThemeConfiguration(t.name, t.fields))
    val themeInheritanceAction: InheritanceAction[Option[ApiThemeConfiguration]] = InheritanceAction[Option[ApiThemeConfiguration]](
      forRoot = _ ⇒ None, // The Frontpage has never a theme
      forFallback = maybeThemeMapping,
      forMatching = maybeThemeMapping
    )
    inheritanceCalculator.forChannel[Option[ApiThemeConfiguration]](rawChannel, themeInheritanceAction, c ⇒ c.config.theme.exists(_.name.isDefined))
  }

  private[converter] def apiMetaConfigurationFromRawChannel(rawChannel: RawChannel): Option[ApiMetaConfiguration] = {
    rawChannel.config.metadata.map(metadata => ApiMetaConfiguration(
      title = metadata.title.filter(_.nonEmpty),
      description = metadata.description.filter(_.nonEmpty),
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
      adIndicator = rawChannel.config.header.map(_.adIndicator),
      showFallbackAds = Some(rawChannel.config.commercial.showFallbackAds)
    )
  }

  private[converter] def apiSponsoringConfigurationFromRawChannel(rawChannel: RawChannel): ApiSponsoringConfiguration = {
    ApiSponsoringConfiguration(
      name = rawChannel.config.sponsoring.logo,
      logo = rawChannel.config.sponsoring.logo,
      slogan = rawChannel.config.sponsoring.slogan,
      hidden = Some(rawChannel.config.sponsoring.hidden),
      link = rawChannel.config.sponsoring.link.map(ref ⇒ ApiReference(ref.label, ref.path)),
      brandstation = rawChannel.config.sponsoring.brandstation
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
      hidden = rawChannel.config.header.map(_.hidden),
      sectionReferences = Some(apiSectionReferences),
      headerReference = rawChannel.config.header.flatMap(_.headerReference).map(ref ⇒ ApiReference(ref.label, ref.path)),
      sloganReference = rawChannel.config.header.flatMap(_.sloganReference).map(ref ⇒ ApiReference(ref.label, ref.path))
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
        showWebExtended = Some(rawChannelCommercial.contentTaboola.showWebExtended),
        showNetwork = Some(rawChannelCommercial.contentTaboola.showNetwork)
      ))
    )
}
