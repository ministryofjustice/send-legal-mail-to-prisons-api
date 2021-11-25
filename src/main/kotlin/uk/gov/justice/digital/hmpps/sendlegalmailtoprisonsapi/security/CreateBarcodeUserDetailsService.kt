package uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.security

import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.User
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Component

@Component
class CreateBarcodeUserDetailsService(private val jwtService: JwtService) :
  UserDetailsService {
  override fun loadUserByUsername(token: String): UserDetails =
    token
      .takeIf { jwtService.validateToken(it) }
      ?.let { jwtService.subject(it) }
      ?.takeIf { username -> username.isNotBlank() }
      ?.let { username -> User(username, "n/a", mutableListOf(SimpleGrantedAuthority("ROLE_SLM_CREATE_BARCODE"))) }
      ?: throw UsernameNotFoundException("Token $token is invalid")
}
