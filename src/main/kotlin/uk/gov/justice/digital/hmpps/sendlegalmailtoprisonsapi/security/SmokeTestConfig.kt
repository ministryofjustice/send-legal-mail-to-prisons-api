package uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.security

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding

@ConstructorBinding
@ConfigurationProperties(prefix = "app.smoketest")
data class SmokeTestConfig(
  val msjSecret: String?,
  val lsjSecret: String?,
)
