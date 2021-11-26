package uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import java.time.Clock

@SpringBootApplication
@ConfigurationPropertiesScan
class SendLegalMailToPrisonsApi {

  @Bean
  fun clock(): Clock = Clock.systemUTC()
}

fun main(args: Array<String>) {
  runApplication<SendLegalMailToPrisonsApi>(*args)
}
