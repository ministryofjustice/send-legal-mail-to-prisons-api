package uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.onetimecode

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding
import java.time.Duration

@ConstructorBinding
@ConfigurationProperties(prefix = "app.one-time-code")
data class OneTimeCodeConfig(
  val codeExpiry: Duration,
  val codeLength: Int,
  val supportedCharacters: List<Char>,
  val maxAttempts: Int,
)
