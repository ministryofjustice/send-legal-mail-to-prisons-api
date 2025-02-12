package uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.cjsm

import mu.KotlinLogging
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.CreateBucketRequest
import java.net.URI

private val log = KotlinLogging.logger {}

@Configuration
class S3ClientsConfig(
  private val s3Config: S3Config,
) {

  @Bean
  @ConditionalOnExpression("T(org.springframework.util.StringUtils).isEmpty('\${app.s3.localstack-url:}')")
  fun amazonS3(): S3Client = S3Client.builder()
    .region(Region.of(s3Config.region))
    .credentialsProvider(DefaultCredentialsProvider.create())
    .build()

  @Bean
  @ConditionalOnProperty(name = ["app.s3.localstack-url"])
  fun amazonS3Test(): S3Client = S3Client.builder()
    .region(Region.of(s3Config.region))
    .endpointOverride(URI.create(s3Config.localstackUrl))
    .credentialsProvider(
      StaticCredentialsProvider.create(
        AwsBasicCredentials.builder().accessKeyId("any").secretAccessKey("any").build(),
      ),
    )
    .forcePathStyle(true)
    .build()
    .also {
      val request = CreateBucketRequest.builder()
        .bucket(s3Config.bucketName).build()

      runCatching {
        it.createBucket(request)
      }.onFailure { log.info("Failed to create S3 bucket ${s3Config.bucketName} due to error ${it.message}") }
    }
}
