package uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.security

import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyNoMoreInteractions
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyString
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.UsernameNotFoundException

class CreateBarcodeUserDetailsServiceTest {

  private val jwtService: JwtService = mock()
  private val createBarcodeUserDetailsService = CreateBarcodeUserDetailsService(jwtService)

  @Test
  fun `should create user details for valid token`() {
    whenever(jwtService.validateToken(anyString())).thenReturn(true)
    whenever(jwtService.subject(anyString())).thenReturn("some.user@company.com.cjsm.net")

    val userDetails = createBarcodeUserDetailsService.loadUserByUsername("some-token")

    assertThat(userDetails.username).isEqualTo("some.user@company.com.cjsm.net")
    assertThat(userDetails.authorities).containsExactly(SimpleGrantedAuthority("ROLE_SLM_CREATE_BARCODE"))
    verify(jwtService).validateToken("some-token")
    verify(jwtService).subject("some-token")
  }

  @Test
  fun `should throw for invalid token`() {
    whenever(jwtService.validateToken(anyString())).thenReturn(false)

    assertThatThrownBy { createBarcodeUserDetailsService.loadUserByUsername("some-token") }
      .isInstanceOf(UsernameNotFoundException::class.java)
      .hasMessageContaining("some-token")

    verify(jwtService).validateToken("some-token")
    verifyNoMoreInteractions(jwtService)
  }

  @Test
  fun `should throw if there is no username`() {
    whenever(jwtService.validateToken(anyString())).thenReturn(true)
    whenever(jwtService.subject(anyString())).thenReturn("")

    assertThatThrownBy { createBarcodeUserDetailsService.loadUserByUsername("some-token") }
      .isInstanceOf(UsernameNotFoundException::class.java)
      .hasMessageContaining("some-token")

    verify(jwtService).validateToken("some-token")
    verify(jwtService).subject("some-token")
  }
}
