package uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.security

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "app.smoketest")
data class SmokeTestConfig(
  val msjSecret: String?,
  val lsjSecret: String?,
)
