package uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.security

import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component

@Component
class SecurityUserContext {
  val authentication: AuthAwareAuthenticationToken?
    get() = with(SecurityContextHolder.getContext().authentication) {
      when (this) {
        is AuthAwareAuthenticationToken -> this
        else -> null
      }
    }

  val principal: String
    get() {
      return if (authentication?.principal != null) {
        authentication?.principal!!
      } else {
        "anonymous"
      }
    }
}
