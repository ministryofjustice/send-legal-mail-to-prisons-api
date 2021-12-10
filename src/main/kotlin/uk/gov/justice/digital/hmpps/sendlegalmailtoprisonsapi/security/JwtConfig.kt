package uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.security

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding
import java.time.Duration

@ConstructorBinding
@ConfigurationProperties(prefix = "app.jwt")
data class JwtConfig(
  val privateKey: String,
  val publicKey: String,
  val expiry: Duration,
)
