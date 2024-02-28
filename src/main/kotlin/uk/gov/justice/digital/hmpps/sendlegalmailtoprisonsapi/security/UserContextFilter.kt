package uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.security

import jakarta.servlet.Filter
import jakarta.servlet.FilterChain
import jakarta.servlet.FilterConfig
import jakarta.servlet.ServletException
import jakarta.servlet.ServletRequest
import jakarta.servlet.ServletResponse
import jakarta.servlet.http.HttpServletRequest
import org.springframework.core.annotation.Order
import org.springframework.http.HttpHeaders
import org.springframework.security.authentication.InsufficientAuthenticationException
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.client.ManageUsersApiClient
import java.io.IOException

@Component
@Order(1)
class UserContextFilter(
  private val manageUsersApiCLient: ManageUsersApiClient,
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

      // this now needs 2 calls - 1 to get the username from manage-users-api and another to get activeCaseloadId by username
      val userName = manageUsersApiCLient.getUsername().username
      userName?.let {
        userContext.caseload = manageUsersApiCLient.getUserDetails(userName).activeCaseLoadId ?: let {
          throw InsufficientAuthenticationException("User ${jwtService.getUser(authToken)} does not have an active caseload")
        }
      } ?: let {
        throw InsufficientAuthenticationException("Could not find username for User ${jwtService.getUser(authToken)}")
      }
    }
    filterChain.doFilter(httpServletRequest, servletResponse)
  }

  override fun init(filterConfig: FilterConfig) {}
  override fun destroy() {}
}
