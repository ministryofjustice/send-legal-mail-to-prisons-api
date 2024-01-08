package uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.onetimecode

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

@ConfigurationProperties(prefix = "app.one-time-code")
data class OneTimeCodeConfig(
  val codeExpiry: Duration,
  val codeLength: Int,
  val supportedCharacters: List<Char>,
  val maxAttempts: Int,
)
