package uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.security

import org.springframework.core.annotation.Order
import org.springframework.http.HttpHeaders
import org.springframework.security.authentication.InsufficientAuthenticationException
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
  private val jwtService: JwtService,
) : Filter {
  @Throws(IOException::class, ServletException::class)
  override fun doFilter(servletRequest: ServletRequest, servletResponse: ServletResponse, filterChain: FilterChain) {
    val httpServletRequest = servletRequest as HttpServletRequest
    val authToken = httpServletRequest.getHeader(HttpHeaders.AUTHORIZATION)
    if (jwtService.isSmokeTestUserToken(authToken)) {
      userContext.authToken = authToken
      userContext.caseload = "SKI"
    } else if (jwtService.isNomisUserToken(authToken)) {
      userContext.authToken = authToken
      userContext.caseload = hmppsAuthClient.getUserDetails().activeCaseLoadId
        ?: let {
          throw InsufficientAuthenticationException("User ${jwtService.getUser(authToken)} does not have an active caseload")
        }
    }
    filterChain.doFilter(httpServletRequest, servletResponse)
  }

  override fun init(filterConfig: FilterConfig) {}
  override fun destroy() {}
}
