# Changelog

## 1.3 (2018-03-20)

- add optional http headers in AbstractService

## 1.2 bugfix (2018-03-16)

- definition which Exceptions will cause the breaker to stay open or allow close. e.g.
    - `HttpClientErrorException` and `HttpRedirectException` will keep the breaker closed (this is good)
    - whereas `HttpServerErrorException` and any other exception may open the breaker

## 1.2 (2017-11-27)

- introduced circuit breaker pattern in AbstractService
- Playframework 2.6.6
- Added Cigger Support

#### Breaking Changes

- AbstractService is now an `abstract class` (instead of a `trait`)
- `ExecutionContexts` should now be `@Injected` everywhere instead of being passed as `implicit` parameters 

## 1.1 (2017-07-21)

- Upgraded to Playframework 2.6.2 

## 1.0 (2017-06-20)

#### Breaking Changes

- Playframework 2.6 (dropped 2.4 & 2.5)
- Scala 2.12 (dropped 2.11)

## 0.22.X (2017-10-26)

### Changes
- [CORE-CLIENT] add Service `ContentBatchService` to allow digger to resolve all Ids of a stage with one call

## 0.21.X (2017-10-19) 

### Changes
- [CORE] add field `validFromDate` to `ApiMetadata`

## 0.20.X (2017-10-17)

### Changes
- [RAW] added field `hideCuratedStageLabel` as part of `RawChannelStageCurated`

## 0.19.0 (2017-???)
### Changes
- [MISSING DATA]

## 0.18.0 (2017-09-19)

### Changes
- [RAW] added param `brandstation` as part of `RawChannelSponsoring`
- [PRESSED] added param `brandstation` as part of `ApiSponsoringConfiguration`

## 0.14.0 (2017-04-12)

#### Breaking Changes
- [CORE]    Delete `PapyrusRepository` and `CurationService (in-sourced to Digger)
- [LEGACY]  Delete Module `Legacy` - Models and Client (da-hood was 100% replaced by Digger)

## 0.13.0 (2017-03-31)

#### Changes
- [RAW] bugfix filter empty map values (overrides)
- [RAW] bugfix correct json.writes of stages
- [CORE] move `Strings.scala` to [UTILS]

#### Breaking

##### Change package for `Strings`

**before**

```
de.welt.contentapi.core.client.utilities.Strings
```

**after**

```
de.welt.contentapi.utils.Strings
```


## 0.12.0 (2017-03-09)

#### Changes
- [RAW] added case class property `master: Boolean` to `RawChannelConfiguration`
- [PRESSED] change impl. of master-channel (inheritance) calculation

#### Breaking
/

## 0.11.0 (2017-02-03)
- Future proof JSON parsing
- Digger can ignore unknown new modules from CMCF
- CMCF can be rolled out with new Modules that are not yet known to Digger will be ignored

#### Changes
- added case class `RawChannelStageIgnored`

## 0.10.0 (2017-02-03)

#### Changes

- `PressedSectionService` now wraps its responses
  - the wrapper contains information about `time`, `status`, `source`, .. of the pressed result
  - those wrapped models are uploaded to S3
  - alternate treatment of backend calls when in `Dev Mode` 
- Changed logging to typesafe's scala-logging (should increase performance and reduce memory)
- minor fixes like typos  

#### Breaking

- the models in S3 and the `PressedSectionService` are now wrapped (JSON changed)

#### Migration

- the Logger automatically does the `log.isLevelEnabled()` and therefore permits calling this directly. Just remove the `if`.
- As for the wrapped JSON: You could simply unwrap it to use the previous models.

## 0.9.0 (2017-01-26)

#### Changes

- use Guice directly instead of Play's DI abstraction.

#### Breaking

- Play's DI and Guice are compatible to some extend, but you should consider using Guice's interfaces if you access the WC/AC DI-Modules directly 

#### Migration

Follow DI as described here https://www.playframework.com/documentation/2.5.x/ScalaDependencyInjection

- migrate your code from `play.api.inject.Module` to `com.google.inject.AbstractModule`
- change from `def bindings(environment: Environment, configuration: Configuration): Seq[Binding[_]]
` to `protected abstract void configure()`
- for module-dependencies, use `install(m: Module)`

## 0.8.0 (2017-01-25)

#### Changes

- migrated to wrapped search endpoint that allows paginated searches

#### Breaking

- none, if migration is applied

#### Migration

- just call `searchResponse.map(_.results)` to unwrap the search result and use the old models
- update the _search_ `endpoint` in your configuration to `/content/wrapped/_search`

## 0.7.0 (2017-01-13)

#### Changes

- params for the `AbstractService` are getting trimmed now (remove leading and trailing white space) Fixes #42

#### Breaking

- type safe rewrite of the `ApiContentSearch`
- made the `RequestHeader` non `Optional`, but initialize with `Seq.empty` instead

## 0.4.0 (2016-10-18)

#### Changes

- `ContentApiQuery`
  - `flags` will be *deprecated* in favour of the new search parameter `flag`: In our API we currently only have two flags: `highlight` and `premium` and the negation of those. With the new `flag` search parameter you can combine/exclude those flags, e.g. `flag: [premium, no_highlight]` or `flag: [no_premium, no_highlight]`. The results are a conjunction of the flags (the terms are *and*ed -> `!premium && !highlight`.
  - `subTypeExcludes` (some of them formerly available through the `flags`) are moving into the `subType` search parameter, use the negated version of the corresponding subType
  - `subType` search parameter will now allow list inputs, allowing you to do things as `subType: [no_broadcast, no_video, no_printImport, no_oembed]`
- added missing default case to `ApiChannel`


## <= 0.3.x

Initial Version
