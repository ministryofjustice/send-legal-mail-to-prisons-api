package uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.config


import io.opentelemetry.api.trace.Span
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.spy
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.IntegrationTest

class ClientTrackingConfigurationTest : IntegrationTest() {
  @Suppress("SpringJavaInjectionPointsAutowiringInspection")
  @Autowired
  private lateinit var clientTrackingInterceptor: ClientTrackingInterceptor

  @BeforeEach
  fun setup() {
  }

  @AfterEach
  fun tearDown() {
  }

  @Test
  fun shouldAddClientIdAndUserNameToInsightTelemetry() {
    val token = jwtAuthHelper.createJwt("bob")
    val req = MockHttpServletRequest()
    req.addHeader(HttpHeaders.AUTHORIZATION, "Bearer $token")
    val res = MockHttpServletResponse()

    val clientTrackingInterceptorSpy = spy(clientTrackingInterceptor)
    val mockSpan = spy(Span.current())
    whenever(clientTrackingInterceptorSpy.getCurrentSpan()).thenReturn(mockSpan)

    clientTrackingInterceptorSpy.preHandle(req, res, "null")
    verify(mockSpan, times(1)).setAttribute("username", "bob")
    verify(mockSpan, times(1)).setAttribute("clientId", "send-legal-mail-client")
  }

  @Test
  fun shouldAddOnlyClientIdIfUsernameNullToInsightTelemetry() {
    val token = jwtAuthHelper.createJwt(null)
    val req = MockHttpServletRequest()
    req.addHeader(HttpHeaders.AUTHORIZATION, "Bearer $token")
    val res = MockHttpServletResponse()

    val clientTrackingInterceptorSpy = spy(clientTrackingInterceptor)
    val mockSpan = spy(Span.current())
    whenever(clientTrackingInterceptorSpy.getCurrentSpan()).thenReturn(mockSpan)

    clientTrackingInterceptorSpy.preHandle(req, res, "null")
    verify(mockSpan, times(1)).setAttribute("clientId", "send-legal-mail-client")
  }
}
