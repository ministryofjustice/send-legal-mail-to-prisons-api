package uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.cjsm

import com.amazonaws.services.s3.AmazonS3
import org.springframework.stereotype.Service

@Service
class CjsmService(private val amazonS3Client: AmazonS3, private val s3Config: S3Config) {

  fun readCjsmOrgs(): String =
    try {
      amazonS3Client.getObjectAsString(s3Config.bucketName, s3Config.cjsmOrgsCsv)
    } catch (ex: Exception) {
      "An error occurred attempting to read ${s3Config.cjsmOrgsCsv} from S3 bucket ${s3Config.bucketName}: ${ex.message}"
    }
}
