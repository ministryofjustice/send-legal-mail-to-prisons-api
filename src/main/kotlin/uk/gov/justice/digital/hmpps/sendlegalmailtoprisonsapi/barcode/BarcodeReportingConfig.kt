package uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.barcode

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "app.barcode-stats-report")
data class BarcodeReportingConfig(
  val recipientEmails: List<String>,
)
