package uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication()
class SendLegalMailToPrisonsApi

fun main(args: Array<String>) {
  runApplication<SendLegalMailToPrisonsApi>(*args)
}
