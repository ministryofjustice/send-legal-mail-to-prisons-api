package uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.security

import com.nhaarman.mockitokotlin2.inOrder
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verifyNoMoreInteractions
import com.nhaarman.mockitokotlin2.whenever
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.client.HmppsAuthClient
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.client.UserDetails
import javax.servlet.FilterChain
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

class UserContextFilterTest {

  private val hmppsAuthClient = mock<HmppsAuthClient>()
  private val userContext = mock<UserContext>()
  private val jwtService = mock<JwtService>()
  private val servletRequest = mock<HttpServletRequest>()
  private val servletResponse = mock<HttpServletResponse>()
  private val filterChain = mock<FilterChain>()

  private val userContextFilter = UserContextFilter(hmppsAuthClient, userContext, jwtService)

  @Test
  fun `should save the auth token and caseload if there is an Authorization header`() {
    whenever(servletRequest.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn("some_token")
    whenever(hmppsAuthClient.getUserDetails()).thenReturn(UserDetails("some_caseload"))
    whenever(jwtService.isNomisUserToken("some_token")).thenReturn(true)

    userContextFilter.doFilter(servletRequest, servletResponse, filterChain)

    // The order is important as the authToken is used to retrieve the caseload
    inOrder(userContext).apply {
      verify(userContext).authToken = "some_token"
      verify(userContext).caseload = "some_caseload"
    }
  }

  @Test
  fun `should not save the auth token or caseload if no Auth header`() {
    whenever(servletRequest.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn(null)
    whenever(jwtService.isNomisUserToken("some_token")).thenReturn(false)

    userContextFilter.doFilter(servletRequest, servletResponse, filterChain)

    verifyNoMoreInteractions(userContext)
  }
}
