package uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.magiclink

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

@ConfigurationProperties(prefix = "app.magiclink")
data class MagicLinkConfig(
  val secretExpiry: Duration,
  val url: String,
)
