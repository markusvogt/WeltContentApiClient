package de.welt.contentapi.core.client.services.contentapi

import com.codahale.metrics.Timer.Context
import com.kenshoo.play.metrics.Metrics
import de.welt.contentapi.core.client.models.{ApiContentSearch, MainTypeParam}
import de.welt.contentapi.core.client.services.exceptions.{HttpClientErrorException, HttpRedirectException, HttpServerErrorException}
import de.welt.contentapi.core.client.services.http.RequestHeaders
import org.mockito.{ArgumentMatcher, Matchers}
import org.mockito.Matchers.anyString
import org.mockito.Mockito.{verify, when}
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.Configuration
import play.api.http.{HeaderNames, Status}
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.{JsLookupResult, JsResult, JsString}
import play.api.libs.ws.ahc.AhcWSRequest
import play.api.libs.ws.{WSAuthScheme, WSClient, WSRequest, WSResponse}

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

class AbstractServiceTest extends PlaySpec with MockitoSugar with Status {

  trait TestScope {

    val mockWsClient: WSClient = mock[WSClient]
    val mockRequest: AhcWSRequest = mock[AhcWSRequest]
    val responseMock: WSResponse = mock[WSResponse]
    val metricsMock: Metrics = mock[Metrics]
    val mockTimerContext: Context = mock[Context]

    when(mockRequest.withHttpHeaders(Matchers.anyVararg())).thenReturn(mockRequest)
    when(mockRequest.withQueryStringParameters(Matchers.anyVararg[(String, String)])).thenReturn(mockRequest)
    when(mockRequest.withAuth(anyString, anyString, Matchers.eq(WSAuthScheme.BASIC))).thenReturn(mockRequest)

    when(mockRequest.get()).thenReturn(Future {
      responseMock
    })

    when(metricsMock.defaultRegistry).thenReturn(new com.codahale.metrics.MetricRegistry())
    when(mockWsClient.url(anyString)).thenReturn(mockRequest)
  }

  trait TestScopeBasicAuth extends TestScope {

    class TestService extends AbstractService[String] {
      override val serviceName: String = "test"
      override val configuration: Configuration = TestServiceBasicAuth.configuration
      override val metrics: Metrics = metricsMock
      override val jsonValidate: (JsLookupResult) => JsResult[String] = json => json.validate[String]
      override val ws: WSClient = mockWsClient

      override protected def initializeMetricsContext(name: String): Context = mockTimerContext
    }

  }

  trait TestScopeApiKey extends TestScope {

    class TestService extends AbstractService[String] {
      override val serviceName: String = "test"
      override val configuration: Configuration = TestServiceApiKeyOverridesBasicAuthConfig.configuration
      override val metrics: Metrics = metricsMock
      override val jsonValidate: (JsLookupResult) => JsResult[String] = json => json.validate[String]
      override val ws: WSClient = mockWsClient

      override protected def initializeMetricsContext(name: String): Context = mockTimerContext
    }

  }

  object TestServiceBasicAuth {
    val configuration = Configuration("welt.api.test" → Map(
      "host" → "http://www.example.com",
      "endpoint" → "/test/%s",
      "credentials.username" → "user",
      "credentials.password" → "pass"
    ))
  }

  object TestServiceApiKeyOverridesBasicAuthConfig {
    val configuration = Configuration("welt.api.test" → Map(
      "host" → "http://www.example.com",
      "endpoint" → "/test/%s",
      "apiKey" → "foo",
      "credentials.username" → "user",
      "credentials.password" → "pass"
    ))
  }

  "AbstractService" should {

    "call the endpoint" in new TestScopeBasicAuth {
      new TestService().get(Seq("fake-id"), Seq.empty)
      verify(mockWsClient).url("http://www.example.com/test/fake-id")
    }

    "forward the X-Unique-Id header" in new TestScopeBasicAuth {
      val headers = Seq(("X-Unique-Id", "0xdeadbeef"))
      new TestService().get(Seq("fake-id"), Seq.empty)(headers, defaultContext)
      verify(mockRequest).withHttpHeaders(("X-Unique-Id", "0xdeadbeef"))
    }

    "forward the basic auth data" in new TestScopeBasicAuth {
      private val service = new TestService()
      service.get(Seq("fake-id"), Seq("foo" -> "bar"))
      val ba = service.config.credentials.right.getOrElse("", "")
      verify(mockRequest).withAuth(ba._1, ba._2, WSAuthScheme.BASIC)
    }

    "forward the api key as header" in new TestScopeApiKey {
      private val service = new TestService()
      service.get(Seq("fake-id"), Seq("foo" -> "bar"))
      service.config.credentials.left.getOrElse("")
      verify(mockRequest).withHttpHeaders(("x-api-key", "foo"))
    }

    "forward the query string data" in new TestScopeBasicAuth {
      new TestService().get(Seq("fake-id"), Seq("foo" -> "bar"))
      verify(mockRequest).withQueryStringParameters(("foo", "bar"))
    }

    "forward the headers" in new TestScopeBasicAuth {
      implicit val requestHeaders: RequestHeaders = Seq("X-Unique-Id" -> "bar")
      new TestService().get(Seq("fake-id"), Seq.empty)
      verify(mockRequest).withHttpHeaders(("X-Unique-Id", "bar"))
    }

    "strip whitespaces and newline from the parameter" in new TestScopeBasicAuth {
      new TestService().get(Seq("with-whitespace \n"))
      verify(mockWsClient).url("http://www.example.com/test/with-whitespace")
    }

    "strip nonbreaking whitespace from parameters" in new TestScopeBasicAuth {
      new TestService().get(Seq("strange-whitespaces"), ApiContentSearch(MainTypeParam(List("\u00A0", " ", "\t", "\n"))).getAllParamsUnwrapped)
      verify(mockRequest).withQueryStringParameters()
    }

    "not strip valid parameters" in new TestScopeBasicAuth {
      val parameters = ApiContentSearch(MainTypeParam(List("param1", "\u00A0param2\u00A0", "\u00A0"))).getAllParamsUnwrapped
      new TestService().get(Seq("strange-whitespaces"), parameters)
      verify(mockRequest).withQueryStringParameters("type" → Seq("param1", "param2").mkString(MainTypeParam().operator))
    }

    "strip empty elements from the query string" in new TestScopeBasicAuth {
      new TestService().get(Seq("x"), Seq("spaces" → " \n", "trim" → "   value   "))
      verify(mockRequest).withQueryStringParameters(("trim", "value"))
    }

    "will return the expected result" in new TestScopeBasicAuth {
      when(responseMock.status).thenReturn(OK)
      when(responseMock.json).thenReturn(JsString("the result"))

      val result: Future[String] = new TestService().get(Seq("x"), Seq.empty)
      val result1 = Await.result(result, 10.second)
      result1 mustBe "the result"
    }

    "will throw a RedirectErrorException when WS status is 301" in new TestScopeBasicAuth {
      when(responseMock.status).thenReturn(MOVED_PERMANENTLY)

      val result: Future[String] = new TestService().get(Seq("requested-id"), Seq.empty)
      the[HttpRedirectException] thrownBy {
        Await.result(result, 10.second)
      } must matchPattern {
        case _: HttpRedirectException ⇒
      }
    }

    "will throw a ClientErrorException when WS status is 404" in new TestScopeBasicAuth {
      when(responseMock.status).thenReturn(NOT_FOUND)
      when(responseMock.header(HeaderNames.CACHE_CONTROL)).thenReturn(Some("cache-header-value"))

      val result: Future[String] = new TestService().get(Seq("requested-id"), Seq.empty)
      the[HttpClientErrorException] thrownBy {
        Await.result(result, 10.second)
      } must matchPattern {
        case HttpClientErrorException(NOT_FOUND, _, _, Some("cache-header-value")) ⇒
      }
    }

    "will throw a ServerErrorException when WS status is 504" in new TestScopeBasicAuth {
      when(responseMock.status).thenReturn(GATEWAY_TIMEOUT)

      val result: Future[String] = new TestService().get(Seq("requested-id"), Seq.empty)
      the[HttpServerErrorException] thrownBy {
        Await.result(result, 10.second)
      } must matchPattern {
        case HttpServerErrorException(GATEWAY_TIMEOUT, _, _) ⇒
      }
    }

    "will invoke metrics" in new TestScopeBasicAuth {
      when(responseMock.status).thenReturn(OK)
      when(responseMock.json).thenReturn(JsString(""))

      val result: Future[String] = new TestService().get(Seq("x"))
      Await.result(result, 10.second)
      verify(mockTimerContext).stop()
    }

  }

}
