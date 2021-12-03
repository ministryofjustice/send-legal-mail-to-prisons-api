package uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.config

import com.microsoft.applicationinsights.web.internal.ThreadContext
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression
import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.HandlerInterceptor
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.security.JwtService
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

@Configuration
@ConditionalOnExpression("T(org.apache.commons.lang3.StringUtils).isNotBlank('\${applicationinsights.connection.string:}')")
class BarcodeTokenClientTrackingConfiguration(private val barcodeTokenClientTrackingInterceptor: BarcodeTokenClientTrackingInterceptor) :
  WebMvcConfigurer {
  override fun addInterceptors(registry: InterceptorRegistry) {
    registry.addInterceptor(barcodeTokenClientTrackingInterceptor).addPathPatterns("/**")
  }
}

@Configuration
class BarcodeTokenClientTrackingInterceptor(private val jwtService: JwtService) : HandlerInterceptor {
  override fun preHandle(request: HttpServletRequest, response: HttpServletResponse, handler: Any): Boolean {
    val properties = ThreadContext.getRequestTelemetryContext().httpRequestTelemetry.properties
    val token = request.getHeader("create-barcode-token")
    token
      ?.let { jwtService.subject(it) }
      ?.also { properties["username"] = it }
    token
      ?.let { jwtService.clientId(it) }
      ?.also { properties["clientId"] = it }
    return true
  }
}
