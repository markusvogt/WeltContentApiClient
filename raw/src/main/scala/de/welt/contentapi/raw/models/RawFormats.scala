package de.welt.contentapi.raw.models

import play.api.libs.functional.syntax._
import play.api.libs.json._

object RawFormats {

  import RawReads._
  import RawWrites._

  implicit lazy val rawChannelIdFormat: Format[RawChannelId] =
    Format[RawChannelId](rawChannelIdReads, rawChannelIdWrites)
  implicit lazy val rawChannelMetaRobotsTagFormat: Format[RawChannelMetaRobotsTag] =
    Format[RawChannelMetaRobotsTag](rawChannelMetaRobotsTagReads, rawChannelMetaRobotsTagWrites)
  implicit lazy val rawChannelMetadataFormat: Format[RawChannelMetadata] =
    Format[RawChannelMetadata](rawChannelMetadataReads, rawChannelMetadataWrites)
  implicit lazy val rawSectionReferenceFormat: Format[RawSectionReference] =
    Format[RawSectionReference](rawSectionReferenceReads, rawSectionReferenceWrites)
  implicit lazy val rawChannelSponsoringFormat: Format[RawChannelSponsoring] =
    Format[RawChannelSponsoring](rawChannelSponsoringReads, rawChannelSponsoringWrites)
  implicit lazy val rawChannelHeaderFormat: Format[RawChannelHeader] =
    Format[RawChannelHeader](rawChannelHeaderReads, rawChannelHeaderWrites)
  implicit lazy val rawChannelContentConfigurationFormat: Format[RawChannelContentConfiguration] =
    Format[RawChannelContentConfiguration](rawChannelContentConfigurationReads, rawChannelContentConfigurationWrites)
  implicit lazy val rawChannelStageConfigurationFormat: Format[RawChannelStageConfiguration] =
    Format[RawChannelStageConfiguration](rawChannelStageConfigurationFormat, rawChannelStageConfigurationFormat)
  implicit lazy val rawChannelCommercialFormat: Format[RawChannelCommercial] =
    Format[RawChannelCommercial](rawChannelCommercialReads, rawChannelCommercialWrites)

  implicit lazy val rawChannelStageCustomFormat: Format[RawChannelStageCustomModule] =
    Format[RawChannelStageCustomModule](rawChannelStageCustomModuleReads, rawChannelStageCustomModuleWrites)
  implicit lazy val rawChannelStageIgnoredFormat: Format[RawChannelStageIgnored] =
    Format[RawChannelStageIgnored](rawChannelStageIgnoredReads, throw new scala.Error("[DEV-ERROR] You should not write IgnoredStages"))
  implicit lazy val rawChannelStageCuratedFormat: Format[RawChannelStageCurated] =
    Format[RawChannelStageCurated](rawChannelStageCuratedReads, rawChannelStageCuratedWrites)
  implicit lazy val rawChannelStageTrackingFormat: Format[RawChannelStageTracking] =
    Format[RawChannelStageTracking](rawChannelStageTrackingReads, rawChannelStageTrackingWrites)
  implicit lazy val rawChannelStageCommercialFormat: Format[RawChannelStageCommercial] =
    Format[RawChannelStageCommercial](rawChannelStageCommercialReads, rawChannelStageCommercialWrites)
  implicit lazy val rawChannelStageFormat: Format[RawChannelStage] =
    Format[RawChannelStage](rawChannelStageReads, rawChannelStageWrites)
  implicit lazy val rawChannelThemeFormat: Format[RawChannelTheme] =
    Format[RawChannelTheme](rawChannelThemeReads, rawChannelThemeWrites)

  implicit lazy val rawMetadataFormat: Format[RawMetadata] =
    Format[RawMetadata](rawMetadataReads, rawMetadataWrites)
  implicit lazy val rawChannelConfigurationFormat: Format[RawChannelConfiguration] =
    Format[RawChannelConfiguration](rawChannelConfigurationReads, rawChannelConfigurationWrites)
}

object RawReads {
  private[models] def jsError(errorMessage: String, json: JsValue) =
    JsError(s"$errorMessage ${Json.prettyPrint(json)}")

  private[models] def jsErrorInvalidData(modelName: String, json: JsValue) =
    jsError(s"$modelName - Could not validate json [a constructor parameter is missing].", json)

  private[models] def jsErrorInvalidJson(json: JsValue) =
    jsError(s"expected js-object, but was:", json)

  implicit lazy val rawChannelIdReads: Reads[RawChannelId] = Json.reads[RawChannelId]
  implicit lazy val rawChannelMetaRobotsTagReads: Reads[RawChannelMetaRobotsTag] = Json.reads[RawChannelMetaRobotsTag]
  implicit lazy val rawChannelMetadataReads: Reads[RawChannelMetadata] = Json.reads[RawChannelMetadata]
  implicit lazy val rawSectionReferenceReads: Reads[RawSectionReference] = Json.reads[RawSectionReference]
  implicit lazy val rawChannelSponsoringReads: Reads[RawChannelSponsoring] = new Reads[RawChannelSponsoring] {
    private lazy val defaults: RawChannelSponsoring = RawChannelSponsoring()
    override def reads(json: JsValue): JsResult[RawChannelSponsoring] = json match {
      case JsObject(underlying) ⇒ JsSuccess(RawChannelSponsoring(
        logo = underlying.get("logo").map(_.as[String]),
        slogan = underlying.get("slogan").map(_.as[String]),
        hidden = underlying.get("hidden").map(_.as[Boolean]).getOrElse(defaults.hidden),
        link = underlying.get("link").map(_.as[RawSectionReference]),
        brandstation = underlying.get("brandstation").map(_.as[String])
      ))
      case err@_ ⇒ jsErrorInvalidJson(err)
    }
  }
  implicit lazy val rawChannelHeaderReads: Reads[RawChannelHeader] = new Reads[RawChannelHeader] {
    private lazy val defaults: RawChannelHeader = RawChannelHeader()
    override def reads(json: JsValue): JsResult[RawChannelHeader] = json match {
      case JsObject(underlying) ⇒ (for {
        hidden ← underlying.get("hidden").map(_.as[Boolean]).orElse(Some(defaults.hidden))
        adIndicator ← underlying.get("adIndicator").map(_.as[Boolean]).orElse(Some(defaults.adIndicator))
      } yield JsSuccess(
        RawChannelHeader(
          logo = underlying.get("logo").map(_.as[String]),
          slogan = underlying.get("slogan").map(_.as[String]),
          label = underlying.get("label").map(_.as[String]),
          sectionReferences = underlying.get("sectionReferences").map(_.as[Seq[RawSectionReference]]),
          hidden = hidden,
          adIndicator = adIndicator,
          sloganReference = underlying.get("sloganReference").map(_.as[RawSectionReference]),
          headerReference = underlying.get("headerReference").map(_.as[RawSectionReference])
        )
      )).getOrElse(jsErrorInvalidData("RawChannelHeader", json))
      case err@_ ⇒ jsErrorInvalidJson(err)
    }
  }
  implicit lazy val rawChannelContentConfigurationReads: Reads[RawChannelContentConfiguration] = Json.reads[RawChannelContentConfiguration]
  implicit lazy val rawChannelStageConfigurationReads: Reads[RawChannelStageConfiguration] = Json.reads[RawChannelStageConfiguration]

  implicit lazy val rawChannelTaboolaCommercialReads: Reads[RawChannelTaboolaCommercial] = new Reads[RawChannelTaboolaCommercial] {
    private lazy val defaults: RawChannelTaboolaCommercial = RawChannelTaboolaCommercial()
    override def reads(json: JsValue): JsResult[RawChannelTaboolaCommercial] = json match {
      case JsObject(underlying) ⇒ (for {
        showNews ← underlying.get("showNews").map(_.as[Boolean]).orElse(Some(defaults.showNews))
        showWeb ← underlying.get("showWeb").map(_.as[Boolean]).orElse(Some(defaults.showWeb))
        showWebExtended ← underlying.get("showWebExtended").map(_.as[Boolean]).orElse(Some(defaults.showWebExtended))
        showNetwork ← underlying.get("showNetwork").map(_.as[Boolean]).orElse(Some(defaults.showNetwork))
      } yield JsSuccess(
        RawChannelTaboolaCommercial(
          showNews = showNews,
          showWeb = showWeb,
          showWebExtended = showWebExtended,
          showNetwork = showNetwork
        )
      )).getOrElse(jsErrorInvalidData("RawChannelTaboolaCommercial", json))
      case err@_ ⇒ jsErrorInvalidJson(err)
    }
  }

  implicit lazy val rawChannelCommercialReads = new Reads[RawChannelCommercial] {
    private lazy val defaults: RawChannelCommercial = RawChannelCommercial()
    override def reads(json: JsValue): JsResult[RawChannelCommercial] = json match {
      case JsObject(underlying) ⇒ (for {
        definesAdTag ← underlying.get("definesAdTag").map(_.as[Boolean]).orElse(Some(defaults.definesAdTag))
        definesVideoAdTag ← underlying.get("definesVideoAdTag").map(_.as[Boolean]).orElse(Some(defaults.definesVideoAdTag))
        contentTaboola ← underlying.get("contentTaboola").map(_.as[RawChannelTaboolaCommercial]).orElse(Some(defaults.contentTaboola))
        showFallbackAds ← underlying.get("showFallbackAds").map(_.as[Boolean]).orElse(Some(defaults.showFallbackAds))
      } yield JsSuccess(
        RawChannelCommercial(
          definesAdTag = definesAdTag,
          definesVideoAdTag = definesVideoAdTag,
          contentTaboola = contentTaboola,
          showFallbackAds = showFallbackAds
        )
      )).getOrElse(jsErrorInvalidData("RawChannelCommercial", json))
      case err@_ ⇒ jsErrorInvalidJson(err)
    }
  }

  implicit lazy val rawChannelStageCustomModuleReads: Reads[RawChannelStageCustomModule] = new Reads[RawChannelStageCustomModule] {
    override def reads(json: JsValue): JsResult[RawChannelStageCustomModule] = json match {
      case JsObject(underlying) ⇒ (for {
        index <- underlying.get("index").map(_.as[Int])
        module <- underlying.get("module").map(_.as[String])
      } yield JsSuccess(
        RawChannelStageCustomModule(
          index = index,
          hidden = underlying.get("hidden").map(_.as[Boolean]).getOrElse(RawChannelStage.HiddenDefault),
          module = module,
          references = underlying.get("references").map(_.as[Seq[RawSectionReference]]),
          overrides = underlying.get("overrides").map(_.as[Map[String, String]]).map(_.filter(EmptyMapValues)),
          trackingName = underlying.get("trackingName").map(_.as[String]),
          link = underlying.get("link").map(_.as[RawSectionReference])
        )
      )).getOrElse(jsErrorInvalidData("RawChannelStageCustomModule", json))
      case err@_ ⇒ jsErrorInvalidJson(err)
    }
  }

  implicit lazy val rawChannelStageCommercialReads: Reads[RawChannelStageCommercial] = Json.reads[RawChannelStageCommercial]
  implicit lazy val rawChannelStageCuratedReads: Reads[RawChannelStageCurated] = Json.reads[RawChannelStageCurated]
  implicit lazy val rawChannelStageTrackingReads: Reads[RawChannelStageTracking] = Json.reads[RawChannelStageTracking]

  implicit lazy val rawChannelStageIgnoredReads = new Reads[RawChannelStageIgnored] {
    override def reads(json: JsValue): JsResult[RawChannelStageIgnored] = json match {
      case JsObject(underlying) ⇒ underlying
        .get("index").map(_.as[Int])
        .map(index ⇒ JsSuccess(RawChannelStageIgnored(index)))
        .getOrElse(jsErrorInvalidData("RawChannelStageIgnored", json))
      case err@_ ⇒ jsErrorInvalidJson(err)
    }
  }

  implicit lazy val rawChannelStageReads = new Reads[RawChannelStage] {
    override def reads(json: JsValue): JsResult[RawChannelStage] = {
      (json \ "type").as[String] match {
        case RawChannelStage.TypeCustomModule =>
          Json.fromJson[RawChannelStageCustomModule](json)
        case RawChannelStage.TypeModule =>
          Json.fromJson[RawChannelStageCustomModule](json)
        case RawChannelStage.TypeCurated ⇒
          Json.fromJson[RawChannelStageCurated](json)
        case RawChannelStage.TypeCommercial =>
          Json.fromJson[RawChannelStageCommercial](json)
        case RawChannelStage.TypeTracking =>
          Json.fromJson[RawChannelStageTracking](json)
        case _ ⇒ Json.fromJson[RawChannelStageIgnored](json)
      }
    }
  }
  implicit lazy val rawChannelThemeReads: Reads[RawChannelTheme] = Json.reads[RawChannelTheme]

  implicit lazy val rawMetadataReads: Reads[RawMetadata] = Json.reads[RawMetadata]
  implicit lazy val rawChannelConfigurationReads: Reads[RawChannelConfiguration] = new Reads[RawChannelConfiguration] {
    private lazy val defaults: RawChannelConfiguration = RawChannelConfiguration()
    override def reads(json: JsValue): JsResult[RawChannelConfiguration] = json match {
      case JsObject(underlying) ⇒
        JsSuccess(RawChannelConfiguration(
          metadata = underlying.get("metadata").map(_.as[RawChannelMetadata]),
          header = underlying.get("header").map(_.as[RawChannelHeader]),
          sponsoring = underlying.get("sponsoring")
            .map(_.as[RawChannelSponsoring])
            .getOrElse(defaults.sponsoring),
          theme = underlying.get("theme").map(_.as[RawChannelTheme]),
          commercial = underlying.get("commercial").map(_.as[RawChannelCommercial]).getOrElse(defaults.commercial),
          content = underlying.get("content").map(_.as[RawChannelContentConfiguration]),
          brand = underlying.get("brand").map(_.as[Boolean]).getOrElse(defaults.brand),
          master = underlying.get("master").map(_.as[Boolean]).getOrElse(defaults.master)
        ))
      case err@_ ⇒ jsErrorInvalidJson(err)
    }
  }

  implicit lazy val rawChannelReads: Reads[RawChannel] = new Reads[RawChannel] {
    override def reads(json: JsValue): JsResult[RawChannel] = json match {
      case JsObject(underlying) ⇒ (for {
        id ← underlying.get("id").map(_.as[RawChannelId])
        config ← underlying.get("config").map(_.as[RawChannelConfiguration])
        metadata ← underlying.get("metadata").map(_.as[RawMetadata])
      } yield {
        JsSuccess(
          RawChannel(
            id = id,
            config = config,
            stageConfiguration = underlying.get("stageConfiguration")
              .map(_.as[RawChannelStageConfiguration]),
            metadata = metadata,
            parent = None,
            children = underlying.get("children").flatMap(_.asOpt[Seq[RawChannel]](seqRawChannelReads)).getOrElse(Nil)
          )
        )
      }
        )
        .getOrElse(jsErrorInvalidData("RawChannel[noChildren]", json))
      case err@_ ⇒ jsErrorInvalidJson(err)
    }
  }

  implicit lazy val seqRawChannelReads: Reads[Seq[RawChannel]] = Reads.seq(rawChannelReads)
}

object RawWrites {
  lazy val filteredMapWrites = new Writes[Map[String, String]] {
    override def writes(m: Map[String, String]): JsValue = Json.toJson(m.filter(EmptyMapValues))
  }

  implicit lazy val rawChannelIdWrites: Writes[RawChannelId] = Json.writes[RawChannelId]
  implicit lazy val rawChannelMetaRobotsTagWrites: Writes[RawChannelMetaRobotsTag] = Json.writes[RawChannelMetaRobotsTag]
  implicit lazy val rawChannelMetadataWrites: Writes[RawChannelMetadata] = Json.writes[RawChannelMetadata]
  implicit lazy val rawSectionReferenceWrites: Writes[RawSectionReference] = Json.writes[RawSectionReference]
  implicit lazy val rawChannelSponsoringWrites: Writes[RawChannelSponsoring] = Json.writes[RawChannelSponsoring]
  implicit lazy val rawChannelHeaderWrites: Writes[RawChannelHeader] = Json.writes[RawChannelHeader]
  implicit lazy val rawChannelContentConfigurationWrites: Writes[RawChannelContentConfiguration] = Json.writes[RawChannelContentConfiguration]
  implicit lazy val rawChannelStageConfigurationWrites: Writes[RawChannelStageConfiguration] = Json.writes[RawChannelStageConfiguration]
  implicit lazy val rawChannelTaboolaCommercialWrites: Writes[RawChannelTaboolaCommercial] = Json.writes[RawChannelTaboolaCommercial]
  implicit lazy val rawChannelCommercialWrites: Writes[RawChannelCommercial] = Json.writes[RawChannelCommercial]

  implicit lazy val rawChannelStageCustomModuleWrites: Writes[RawChannelStageCustomModule] = (
    (__ \ "index").write[Int] and
      OWrites[String](_ ⇒ JsObject(Map("type" → JsString(RawChannelStage.TypeCustomModule)))) and
      (__ \ "hidden").write[Boolean] and
      (__ \ "trackingName").writeNullable[String] and
      (__ \ "link").writeNullable[RawSectionReference] and
      (__ \ "module").write[String] and
      (__ \ "references").writeNullable[Seq[RawSectionReference]] and
      (__ \ "overrides").writeNullable[Map[String, String]](filteredMapWrites)
    ) (unlift(RawChannelStageCustomModule.unapply))

  implicit lazy val rawChannelStageCommercialWrites: Writes[RawChannelStageCommercial] = (
    (__ \ "index").write[Int] and
      OWrites[String](_ ⇒ JsObject(Map("type" → JsString(RawChannelStage.TypeCommercial)))) and
      (__ \ "hidden").write[Boolean] and
      (__ \ "trackingName").writeNullable[String] and
      (__ \ "link").writeNullable[RawSectionReference] and
      (__ \ "format").write[String]
    ) (unlift(RawChannelStageCommercial.unapply))

  implicit lazy val rawChannelStageCuratedWrites: Writes[RawChannelStageCurated] = (
    (__ \ "index").write[Int] and
      OWrites[String](_ ⇒ JsObject(Map("type" → JsString(RawChannelStage.TypeCurated)))) and
      (__ \ "hidden").write[Boolean] and
      (__ \ "trackingName").writeNullable[String] and
      (__ \ "link").writeNullable[RawSectionReference] and
      (__ \ "curatedSectionMapping").write[String] and
      (__ \ "curatedStageMapping").write[String] and
      (__ \ "layout").writeNullable[String] and
      (__ \ "label").writeNullable[String] and
      (__ \ "logo").writeNullable[String] and
      (__ \ "references").writeNullable[Seq[RawSectionReference]]
    ) (unlift(RawChannelStageCurated.unapply))

  implicit lazy val rawChannelStageTrackingWrites: Writes[RawChannelStageTracking] = (
    (__ \ "index").write[Int] and
      OWrites[String](_ ⇒ JsObject(Map("type" → JsString(RawChannelStage.TypeTracking)))) and
      (__ \ "hidden").write[Boolean] and
      (__ \ "trackingName").writeNullable[String] and
      (__ \ "link").writeNullable[RawSectionReference] and
      (__ \ "layout").writeNullable[String] and
      (__ \ "label").writeNullable[String] and
      (__ \ "logo").writeNullable[String] and
      (__ \ "references").writeNullable[Seq[RawSectionReference]] and
      (__ \ "reportName").write[String]
    ) (unlift(RawChannelStageTracking.unapply))

  implicit lazy val rawChannelStageWrites = new Writes[RawChannelStage] {
    override def writes(o: RawChannelStage): JsValue = o match {
      case r: RawChannelStageCustomModule =>
        Json.toJson(r)(rawChannelStageCustomModuleWrites)
      case c: RawChannelStageCommercial =>
        Json.toJson(c)(rawChannelStageCommercialWrites)
      case c: RawChannelStageCurated =>
        Json.toJson(c)(rawChannelStageCuratedWrites)
      case c: RawChannelStageTracking =>
        Json.toJson(c)(rawChannelStageTrackingWrites)
      case err@_ ⇒ throw new IllegalStateException(s"[DEV-ERROR] Missing case-matching for new RawChannelStage: $err")
    }
  }

  implicit lazy val rawChannelThemeWrites: Writes[RawChannelTheme] = Json.writes[RawChannelTheme]
  implicit lazy val rawMetadataWrites: Writes[RawMetadata] = Json.writes[RawMetadata]
  implicit lazy val rawChannelConfigurationWrites: Writes[RawChannelConfiguration] = Json.writes[RawChannelConfiguration]
}

object FullRawChannelWrites {

  import RawWrites._

  implicit lazy val channelWrites: Writes[RawChannel] = (
    (__ \ "id").write[RawChannelId] and
      (__ \ "config").write[RawChannelConfiguration] and
      (__ \ "stageConfiguration").writeNullable[RawChannelStageConfiguration] and
      (__ \ "metadata").write[RawMetadata] and
      (__ \ "parent").lazyWrite(Writes.optionWithNull(PartialRawChannelWrites.writeChannelAsNull)) and // avoid loops
      (__ \ "children").lazyWrite(Writes.seq[RawChannel](channelWrites)) and
      (__ \ "hasChildren").write[Boolean]
    ) (unlift(RawChannel.unapply))

}

object PartialRawChannelWrites {

  import RawWrites._

  implicit lazy val noChildrenWrites: Writes[RawChannel] = new Writes[RawChannel] {
    override def writes(o: RawChannel): JsValue = {

      JsObject(Map(
        "id" → Json.toJson(o.id),
        "hasChildren" → JsBoolean(o.hasChildren),
        "config" → Json.toJson(o.config),
        "metadata" → Json.toJson(o.metadata)
      )
        ++ o.stageConfiguration.map { stageConfiguration ⇒ "stageConfiguration" → Json.toJson(stageConfiguration) }
      )
    }
  }

  implicit lazy val oneLevelOfChildren: Writes[RawChannel] = (
    (__ \ "id").write[RawChannelId] and
      (__ \ "config").write[RawChannelConfiguration] and
      (__ \ "stageConfiguration").writeNullable[RawChannelStageConfiguration] and
      (__ \ "metadata").write[RawMetadata] and
      (__ \ "parent").lazyWrite(Writes.optionWithNull(noChildrenWrites)) and
      (__ \ "children").lazyWrite(Writes.seq[RawChannel](noChildrenWrites)) and
      (__ \ "hasChildren").write[Boolean]
    ) (unlift(RawChannel.unapply))

  implicit lazy val writeChannelAsNull: Writes[RawChannel] = new Writes[RawChannel] {
    override def writes(o: RawChannel): JsValue = JsNull
  }
}

object PartialRawChannelReads {

  import RawReads._

  implicit lazy val noChildrenReads: Reads[RawChannel] = new Reads[RawChannel] {
    override def reads(json: JsValue): JsResult[RawChannel] = json match {
      case JsObject(underlying) ⇒ (for {
        id ← underlying.get("id").map(_.as[RawChannelId])
        config ← underlying.get("config").map(_.as[RawChannelConfiguration])
        metadata ← underlying.get("metadata").map(_.as[RawMetadata])
      } yield {
        JsSuccess(
          RawChannel(
            id = id,
            config = config,
            metadata = metadata,
            stageConfiguration = underlying.get("stageConfiguration")
              .map(_.as[RawChannelStageConfiguration]),
            children = Seq.empty
          ))
      })
        .getOrElse(jsErrorInvalidData("RawChannel[noChildren]", json))
      case err@_ ⇒ jsErrorInvalidJson(err)
    }
  }
}
