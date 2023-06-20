package uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.cjsm

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding
import org.springframework.validation.annotation.Validated

@ConstructorBinding
@ConfigurationProperties(prefix = "app.s3")
@Validated
data class S3Config(
  val bucketName: String = "",
  val bucketArn: String = "",
  val localstackUrl: String = "",
  val region: String = "",
  val cjsmDirectoryCsvName: String = "",
)
