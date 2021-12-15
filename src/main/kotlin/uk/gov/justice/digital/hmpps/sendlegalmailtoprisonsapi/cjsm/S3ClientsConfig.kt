package uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.cjsm

import com.amazonaws.auth.AWSStaticCredentialsProvider
import com.amazonaws.auth.AnonymousAWSCredentials
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.client.builder.AwsClientBuilder
import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.AmazonS3ClientBuilder
import com.amazonaws.services.s3.model.CreateBucketRequest
import mu.KotlinLogging
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

private val log = KotlinLogging.logger {}

@Configuration
class S3ClientsConfig(private val s3Config: S3Config) {

  @Bean
  @ConditionalOnExpression("T(org.springframework.util.StringUtils).isEmpty('\${app.s3.localstack-url:}')")
  fun amazonS3(): AmazonS3 =
    AmazonS3ClientBuilder.standard()
      .withCredentials(AWSStaticCredentialsProvider(BasicAWSCredentials(s3Config.accessKeyId, s3Config.secretAccessKey)))
      .withRegion(s3Config.region)
      .build()

  @Bean
  @ConditionalOnProperty(name = ["app.s3.localstack-url"])
  fun amazonS3Test(): AmazonS3 =
    AmazonS3ClientBuilder.standard()
      .withEndpointConfiguration(AwsClientBuilder.EndpointConfiguration(s3Config.localstackUrl, s3Config.region))
      .withCredentials(AWSStaticCredentialsProvider(AnonymousAWSCredentials()))
      .build()
      .also {
        val request = CreateBucketRequest(s3Config.bucketName, s3Config.region)
        runCatching {
          it.createBucket(request)
        }.onFailure { log.info("Failed to create S3 bucket ${s3Config.bucketName} due to error ${it.message}") }
      }
}
