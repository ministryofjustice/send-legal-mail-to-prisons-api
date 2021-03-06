package uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.security

import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyString
import org.mockito.kotlin.inOrder
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.springframework.http.HttpHeaders
import org.springframework.security.authentication.InsufficientAuthenticationException
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
    whenever(servletRequest.getHeader(anyString())).thenReturn("some_token")
    whenever(hmppsAuthClient.getUserDetails()).thenReturn(UserDetails("some_caseload"))
    whenever(jwtService.isNomisUserToken(anyString())).thenReturn(false)
    whenever(jwtService.isNomisUserToken(anyString())).thenReturn(true)

    userContextFilter.doFilter(servletRequest, servletResponse, filterChain)

    verify(servletRequest).getHeader(HttpHeaders.AUTHORIZATION)
    verify(hmppsAuthClient).getUserDetails()
    verify(jwtService).isSmokeTestUserToken("some_token")
    verify(jwtService).isNomisUserToken("some_token")
    // The order is important as the authToken is used to retrieve the caseload
    inOrder(userContext).apply {
      verify(userContext).authToken = "some_token"
      verify(userContext).caseload = "some_caseload"
    }
  }

  @Test
  fun `should save token and caseload for a smoketest user`() {
    whenever(servletRequest.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn("some_token")
    whenever(jwtService.isSmokeTestUserToken(anyString())).thenReturn(true)

    userContextFilter.doFilter(servletRequest, servletResponse, filterChain)

    verify(jwtService).isSmokeTestUserToken("some_token")
    verify(userContext).authToken = "some_token"
    verify(userContext).caseload = "SKI"
    verifyNoInteractions(hmppsAuthClient)
  }

  @Test
  fun `should not save the auth token or caseload if no Auth header`() {
    whenever(jwtService.isSmokeTestUserToken(anyString())).thenReturn(false)
    whenever(jwtService.isNomisUserToken(anyString())).thenReturn(false)

    userContextFilter.doFilter(servletRequest, servletResponse, filterChain)

    verifyNoInteractions(hmppsAuthClient)
    verifyNoInteractions(userContext)
  }

  @Test
  fun `should throw if the user has no active caseload returned from auth`() {
    whenever(servletRequest.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn("some_token")
    whenever(hmppsAuthClient.getUserDetails()).thenReturn(UserDetails())
    whenever(jwtService.isNomisUserToken(anyString())).thenReturn(true)

    assertThatThrownBy {
      userContextFilter.doFilter(servletRequest, servletResponse, filterChain)
    }.isInstanceOf(InsufficientAuthenticationException::class.java)

    verify(jwtService).isNomisUserToken("some_token")
    verify(hmppsAuthClient).getUserDetails()
  }
}
