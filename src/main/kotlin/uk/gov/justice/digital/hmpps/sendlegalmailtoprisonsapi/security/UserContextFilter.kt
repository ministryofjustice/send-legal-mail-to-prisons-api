package uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.security

import org.springframework.core.annotation.Order
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.client.HmppsAuthClient
import java.io.IOException
import javax.servlet.Filter
import javax.servlet.FilterChain
import javax.servlet.FilterConfig
import javax.servlet.ServletException
import javax.servlet.ServletRequest
import javax.servlet.ServletResponse
import javax.servlet.http.HttpServletRequest

@Component
@Order(1)
class UserContextFilter(
  private val hmppsAuthClient: HmppsAuthClient,
  private val userContext: UserContext,
  private val jwtService: JwtService
) : Filter {
  @Throws(IOException::class, ServletException::class)
  override fun doFilter(servletRequest: ServletRequest, servletResponse: ServletResponse, filterChain: FilterChain) {
    val httpServletRequest = servletRequest as HttpServletRequest
    val authToken = httpServletRequest.getHeader(HttpHeaders.AUTHORIZATION)
    if (jwtService.isHmppsUserToken(authToken)) {
      userContext.authToken = authToken
      userContext.caseload = hmppsAuthClient.getUserDetails().activeCaseLoadId
    }
    filterChain.doFilter(httpServletRequest, servletResponse)
  }

  override fun init(filterConfig: FilterConfig) {}
  override fun destroy() {}
}