package uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.config

import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.trace.Tracer
import io.opentelemetry.sdk.testing.junit5.OpenTelemetryExtension
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.IntegrationTest

class BarcodeTokenClientTrackingInterceptorTest : IntegrationTest() {

  @Autowired
  private lateinit var barcodeTokenClientTrackingInterceptor: BarcodeTokenClientTrackingInterceptor

  private val tracer: Tracer = otelTesting.openTelemetry.getTracer("test")

  @Test
  fun `should add client and username to telemetry`() {
    val token = jwtService.generateToken("some.email@company.com", "Some Organisation")
    val req = MockHttpServletRequest()
    req.addHeader("create-barcode-token", token)
    val res = MockHttpServletResponse()
    tracer.spanBuilder("span").startSpan().run {
      makeCurrent().use { barcodeTokenClientTrackingInterceptor.preHandle(req, res, "null") }
      end()
    }
    otelTesting.assertTraces().hasTracesSatisfyingExactly({ t ->
      t.hasSpansSatisfyingExactly({
        it.hasAttribute(AttributeKey.stringKey("username"), "some.email@company.com")
        it.hasAttribute(AttributeKey.stringKey("clientId"), "send-legal-mail")
      })
    })
  }

  private companion object {
    @JvmStatic
    @RegisterExtension
    private val otelTesting: OpenTelemetryExtension = OpenTelemetryExtension.create()
  }
}
