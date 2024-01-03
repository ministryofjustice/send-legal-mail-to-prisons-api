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
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.IntegrationTest

class BarcodeTokenClientTrackingInterceptorTest : IntegrationTest() {

  @Autowired
  private lateinit var barcodeTokenClientTrackingInterceptor: BarcodeTokenClientTrackingInterceptor

  @BeforeEach
  fun setup() {
  }

  @AfterEach
  fun tearDown() {
  }

  @Test
  fun `should add client and username to telemetry`() {
    val token = jwtService.generateToken("some.email@company.com", "Some Organisation")
    val req = MockHttpServletRequest()
    req.addHeader("create-barcode-token", token)
    val res = MockHttpServletResponse()

    val clientTrackingInterceptorSpy = spy(barcodeTokenClientTrackingInterceptor)
    val mockSpan = spy(Span.current())
    whenever(clientTrackingInterceptorSpy.getCurrentSpan()).thenReturn(mockSpan)

    clientTrackingInterceptorSpy.preHandle(req, res, "null")
    verify(mockSpan, times(1)).setAttribute("username", "some.email@company.com")
    verify(mockSpan, times(1)).setAttribute("clientId", "send-legal-mail")
  }
}
