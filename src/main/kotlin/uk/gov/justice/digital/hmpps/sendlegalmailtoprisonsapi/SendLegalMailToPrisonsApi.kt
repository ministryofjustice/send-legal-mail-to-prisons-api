package uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication

@SpringBootApplication
@ConfigurationPropertiesScan
class SendLegalMailToPrisonsApi

fun main(args: Array<String>) {
  runApplication<SendLegalMailToPrisonsApi>(*args)
}
