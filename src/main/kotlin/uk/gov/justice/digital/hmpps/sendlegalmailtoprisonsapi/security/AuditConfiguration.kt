package uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.security

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.domain.AuditorAware
import org.springframework.data.jpa.repository.config.EnableJpaAuditing
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.stereotype.Component
import java.util.Optional

@Configuration
@EnableJpaAuditing
class AuditConfiguration {
  @Bean
  fun auditorAware(authenticationFacade: AuthenticationFacade): AuditorAware<String> = AuditorAware(authenticationFacade)
}

class AuditorAware(private val authenticationFacade: AuthenticationFacade) : AuditorAware<String> {
  override fun getCurrentAuditor(): Optional<String> = Optional.ofNullable(authenticationFacade.currentUsername)
}

interface AuthenticationFacade {
  val currentUsername: String?
}

@Component
class UserDetails : AuthenticationFacade {
  override val currentUsername: String?
    get() = when (val userPrincipal: Any? = getUserPrincipal()) {
      is String -> userPrincipal.toString()
      is UserDetails -> userPrincipal.username
      is Map<*, *> -> userPrincipal.get("username").toString()
      else -> null
    }

  private fun getUserPrincipal() = SecurityContextHolder.getContext().authentication?.principal
}
