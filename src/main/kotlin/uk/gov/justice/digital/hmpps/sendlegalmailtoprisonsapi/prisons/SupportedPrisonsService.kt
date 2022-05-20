package uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.prisons

import org.springframework.stereotype.Service

@Service
class SupportedPrisonsService(private val supportedPrisonsRepository: SupportedPrisonsRepository) {

  fun findSupportedPrisonCodes(): List<String> = supportedPrisonsRepository.findByActive(true).map { it.code }.sorted()
}
