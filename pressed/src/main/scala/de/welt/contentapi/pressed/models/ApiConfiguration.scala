package de.welt.contentapi.pressed.models

import de.welt.contentapi.core.models.ApiReference

/**
  * Configuration for a content or section page. All configs are optional.
  * This means that they can be overwritten (by ConfigMcConfigFace) but not required. All clients must define some kind
  * of fallback or default values.
  *
  * @param meta       configuration for <meta> tag overrides
  * @param commercial commercial configuration
  * @param sponsoring sponsoring is part of the page header. E.g. Formel1
  * @param header     (content) page header configuration. Not the real page header.
  * @param theme      theme of the page. This contains only a mapping value.
  */
case class ApiConfiguration(meta: Option[ApiMetaConfiguration] = None,
                            commercial: Option[ApiCommercialConfiguration] = None,
                            sponsoring: Option[ApiSponsoringConfiguration] = None,
                            header: Option[ApiHeaderConfiguration] = None,
                            theme: Option[ApiThemeConfiguration] = None)

/**
  * <meta> configuration for content or section pages
  *
  * @param title             <title> override
  * @param description       <meta> description override
  * @param tags              <meta> keyword override & commercial tagging
  * @param contentMetaRobots override `<meta name="robots">` tag only for all content pages of the channel.
  * @param sectionMetaRobots override `<meta name="robots">` tag only for the section page of the channel.
  */
case class ApiMetaConfiguration(title: Option[String] = None,
                                description: Option[String] = None,
                                tags: Option[Seq[String]] = None,
                                contentMetaRobots: Option[ApiMetaRobots] = None,
                                sectionMetaRobots: Option[ApiMetaRobots] = None) {
  lazy val unwrappedTags: Seq[String] = tags.getOrElse(Nil)
}

/**
  * <meta name="robots" content="index,follow,noodp">
  *
  * @param noIndex  `true` == 'noIndex' & `false` == 'index'
  * @param noFollow `true` == 'noFollow' & `false` == 'follow'
  */
case class ApiMetaRobots(noIndex: Option[Boolean] = None, noFollow: Option[Boolean] = None)

/**
  * Some overrides for commercial settings (ASMI). Per default all commercial configuration based on the section path.
  *
  * @param pathForAdTag      path used to build the ad tag in client
  * @param pathForVideoAdTag path used to build the video ad tag in client
  * @param thirdParty        controls 3rd-party commercial scripts
  * @param adIndicator       Indicator for an advertorial or mark as advertisement. Used for: display the label 'Anzeige'.
  * @param showFallbackAds   Control to display fallback ads if ASMI fails to deliver their own for several ad formats (m-rectangle, skyscraper, ...)
  */
case class ApiCommercialConfiguration(pathForAdTag: Option[String] = None,
                                      pathForVideoAdTag: Option[String] = None,
                                      thirdParty: Option[ApiCommercial3rdPartyConfiguration] = None,
                                      adIndicator: Option[Boolean] = None,
                                      showFallbackAds: Option[Boolean] = Some(true))

/**
  * Enable/Disable 3rd-Party commercial scripts on section/content pages.
  *
  * CARE:
  * All Default values are defined by CMCF.
  *
  * @param contentTaboola Controls Taboola Scripts below the article text.
  */
case class ApiCommercial3rdPartyConfiguration(contentTaboola: Option[ApiCommercialTaboolaConfiguration] = None)

/**
  * Enable/Disable Taboola scripts on each content page of the channel. All Default values are defined by CMCF.
  * Do not set Default values here.
  *
  * @param showNews        "Mehr aus dem Web". Taboola named it 'Below Article Thumbnails'
  * @param showWeb         "Neues aus der Redaktion". Taboola named it 'Below Article Thumbnails 2nd'
  * @param showWebExtended "Auch interessant". Taboola named it 'Below Article Thumbnails 3rd'
  * @param showNetwork     "Neues aus unserem Netzwerk". Taboola named it 'Exchange Below Article Thumbnails'
  */
case class ApiCommercialTaboolaConfiguration(showNews: Option[Boolean] = None,
                                             showWeb: Option[Boolean] = None,
                                             showWebExtended: Option[Boolean] = None,
                                             showNetwork: Option[Boolean] = None)

/**
  * Sponsoring of section and content pages. The impl is part of the client.
  *
  * @param name         name of the branding. Need for mapping.
  * @param logo         name of the sponsoring logo. Need for mapping. Not a real image path.
  * @param slogan       optional slogan of the sponsoring.
  * @param hidden       name of the branding. Need for mapping.
  * @param link         Optional link for the logo.
  * @param brandstation Optional brandstation type. Need for mapping.
  */
case class ApiSponsoringConfiguration(@deprecated("Renaming. Use logo instead", since = "01/2017")
                                      name: Option[String] = None,
                                      logo: Option[String] = None,
                                      slogan: Option[String] = None,
                                      hidden: Option[Boolean] = None,
                                      link: Option[ApiReference] = None,
                                      brandstation: Option[String] = None) {
  lazy val isHidden: Boolean = hidden.getOrElse(false)
}

/**
  * Some configuration for the section or content page header. Not the real page header.
  *
  * @param label             label for the section/content page. Used by escenic section title but can be overridden by janus/cmcf
  * @param logo              mapping name for the client. When a logo is configured by janus/cmcf it overrides the label
  * @param slogan            optional slogan for the label/logo
  * @param sectionReferences section refs for linking
  * @param hidden            Hide the complete channel header. Default == `false`
  * @param sloganReference   Optional link for the slogan.
  * @param headerReference   Optional link for the logo/label.
  *                          Link logic (pseudo-code):  `headerReference.getOrElse(Master-Channel-Link)`
  */
case class ApiHeaderConfiguration(label: Option[String] = None,
                                  logo: Option[String] = None,
                                  slogan: Option[String] = None,
                                  sectionReferences: Option[Seq[ApiReference]] = None,
                                  hidden: Option[Boolean] = None,
                                  sloganReference: Option[ApiReference] = None,
                                  headerReference: Option[ApiReference] = None) {
  lazy val unwrappedSectionReferences: Seq[ApiReference] = sectionReferences.getOrElse(Nil)
  lazy val isHidden: Boolean = hidden.getOrElse(false)
}

/**
  * Theme of the section or content page. Mostly for some background color changes e.g. mediathek. This is only
  * the name of the theme. The impl is part of the client.
  *
  * @param name   name of the theme. Need for mapping.
  * @param fields optional settings/hints/configuration of the theme
  */
case class ApiThemeConfiguration(name: Option[String] = None, fields: Option[Map[String, String]] = None) {
  lazy val unwrappedFields: Map[String, String] = fields.getOrElse(Map.empty[String, String])
}
