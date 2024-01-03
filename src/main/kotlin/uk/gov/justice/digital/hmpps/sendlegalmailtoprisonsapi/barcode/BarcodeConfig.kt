package uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.barcode

import org.hibernate.validator.constraints.Range
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.validation.annotation.Validated
import java.time.Duration

@ConfigurationProperties(prefix = "app.barcode")
@Validated
data class BarcodeConfig(
  val expiry: Duration,
  @get:Range(min = 0, max = 100)
  val randomCheckPercentage: Int,
)
