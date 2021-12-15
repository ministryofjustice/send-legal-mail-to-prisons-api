package uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.cjsm

import com.amazonaws.services.s3.AmazonS3
import org.springframework.stereotype.Service

@Service
class CjsmOrgsService(private val amazonS3Client: AmazonS3, private val s3Config: S3Config) {

  fun readCjsmOrgs() =
    amazonS3Client.getObjectAsString(s3Config.bucketName, "cjsm-orgs.csv")
}
