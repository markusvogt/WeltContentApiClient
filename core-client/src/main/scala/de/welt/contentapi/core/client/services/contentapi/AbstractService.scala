package de.welt.contentapi.core.client.services.contentapi

import com.codahale.metrics.{MetricRegistry, Timer}
import com.kenshoo.play.metrics.Metrics
import de.welt.contentapi.core.client.services.configuration.ServiceConfiguration
import de.welt.contentapi.core.client.services.exceptions.{HttpClientErrorException, HttpRedirectException, HttpServerErrorException}
import de.welt.contentapi.core.client.services.http.RequestHeaders
import de.welt.contentapi.utils.{Loggable, Strings}
import play.api.Configuration
import play.api.http.{HeaderNames, Status}
import play.api.libs.json.{JsError, JsLookupResult, JsResult, JsSuccess}
import play.api.libs.ws.{WSAuthScheme, WSClient, WSRequest}
import play.api.mvc.Headers

import scala.concurrent.{ExecutionContext, Future}

trait AbstractService[T] extends Strings with Loggable with Status with HeaderNames {

  val HEADER_API_KEY = "x-api-key"

  /** these need to be provided by the implementing services */
  val ws: WSClient
  val metrics: Metrics
  val configuration: Configuration

  /**
    * this service's name
    *
    * @return service name
    */
  def serviceName: String

  /**
    * This must provide a [[ServiceConfiguration]]. It will be used to
    * configure the REST request.
    *
    * @return a [[ServiceConfiguration]]
    */
  protected[contentapi] def config: ServiceConfiguration = ServiceConfiguration(s"welt.api.$serviceName", configuration)

  /**
    * This must provide a function that maps a [[JsLookupResult]] to a [[JsResult]].
    * Overriding this outside this trait is required, since the macro-mechanism that
    * takes care of the JSON parsing does not work with generic types `T`
    *
    * @return a [[JsResult]]
    */
  def jsonValidate: JsLookupResult => JsResult[T]

  /**
    * @param urlArguments            string interpolation arguments for endpoint. e.g. /foo/%s/bar/%s see [[java.lang.String#format}]]
    * @param parameters              URL parameters to be sent with the request
    * @param forwardedRequestHeaders forwarded request headers from the controller e.g. API key
    * @param executionContext        pass in an execution context for async processing/mapping
    * @return
    */
  def get(urlArguments: Seq[String] = Nil,
          parameters: Seq[(String, String)] = Nil)
         (implicit forwardedRequestHeaders: RequestHeaders = Seq.empty, executionContext: ExecutionContext): Future[T] = {

    def parseJson(json: JsLookupResult): T = jsonValidate(json) match {
      case JsSuccess(value, _) => value
      case err@JsError(_) => throw new IllegalStateException(err.toString)
    }

    val context = initializeMetricsContext(config.serviceName)

    val url: String = config.host + config.endpoint.format(urlArguments.map(stripWhiteSpaces).filter(_.nonEmpty): _*)

    val filteredParameters = parameters.map { case (k, v) ⇒ k → stripWhiteSpaces(v) }.filter(_._2.nonEmpty)

    val getRequest: WSRequest = ws.url(url)
      .withQueryStringParameters(filteredParameters: _*)

    val authenticatedGetRequest = config.credentials match {
      case Right(basicAuth) ⇒ getRequest
        .withAuth(username = basicAuth._1, password = basicAuth._2, WSAuthScheme.BASIC)

      case Left(apiKey) ⇒ getRequest
        .withHttpHeaders(HEADER_API_KEY → apiKey)
    }
    log.debug(s"HTTP GET to ${authenticatedGetRequest.uri}")

    authenticatedGetRequest
      .withHttpHeaders(forwardHeaders(forwardedRequestHeaders): _*)
      .get().map { response ⇒

      context.stop()

        response.status match {
          case OK ⇒ parseJson(response.json.result)
          case status if (300 until 400).contains(status) ⇒ throw HttpRedirectException(url, response.statusText)
          case status if (400 until 500).contains(status) ⇒ throw HttpClientErrorException(status, response.statusText, url, response.header(CACHE_CONTROL))
          case status ⇒ throw HttpServerErrorException(status, response.statusText, url)
        }
      }
  }

  /**
    * headers to be forwarded from client to server, e.g. the `X-Unique-Id`
    *
    * @param maybeHeaders [[Headers]] from the incoming [[play.api.mvc.Request]]
    * @return tuples of type String for headers to be forwarded
    */
  def forwardHeaders(maybeHeaders: RequestHeaders): RequestHeaders = {
    maybeHeaders.collect { case tuple@("X-Unique-Id", _) ⇒ tuple }
  }

  protected def initializeMetricsContext(name: String): Timer.Context = {
    metrics.defaultRegistry.timer(MetricRegistry.name(s"service.$name", "requestTimer")).time()
  }
}
