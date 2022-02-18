package uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.config

import com.microsoft.applicationinsights.web.internal.RequestTelemetryContext
import com.microsoft.applicationinsights.web.internal.ThreadContext
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.IntegrationTest

class BarcodeTokenClientTrackingInterceptorTest : IntegrationTest() {

  @Autowired
  private lateinit var barcodeTokenClientTrackingInterceptor: BarcodeTokenClientTrackingInterceptor

  @BeforeEach
  fun setup() {
    ThreadContext.setRequestTelemetryContext(RequestTelemetryContext(1L))
  }

  @AfterEach
  fun tearDown() {
    ThreadContext.remove()
  }
  @Test
  fun `should add client and username to telemetry`() {
    val token = jwtService.generateToken("some.email@company.com", "Some Organisation")
    val req = MockHttpServletRequest()
    req.addHeader("create-barcode-token", token)
    val res = MockHttpServletResponse()
    barcodeTokenClientTrackingInterceptor.preHandle(req, res, "null")
    val insightTelemetry = ThreadContext.getRequestTelemetryContext().httpRequestTelemetry.properties

    assertThat(insightTelemetry["username"]).isEqualTo("some.email@company.com")
    assertThat(insightTelemetry["clientId"]).isEqualTo("send-legal-mail")
  }
}
