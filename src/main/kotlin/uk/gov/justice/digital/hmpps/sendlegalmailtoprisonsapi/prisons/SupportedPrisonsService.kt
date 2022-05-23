package uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.prisons

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.client.PrisonRegisterClient

@Service
class SupportedPrisonsService(
  private val supportedPrisonsRepository: SupportedPrisonsRepository,
  private val prisonRegisterClient: PrisonRegisterClient,
) {

  fun findSupportedPrisonCodes(): List<String> =
    supportedPrisonsRepository.findByActive(true)
      .map { it.code }
      .sorted()

  fun addPrisonCode(prisonCode: String): String? =
    prisonRegisterClient.getPrison(prisonCode)
      ?.takeIf { prisonDto -> prisonDto.active }
      ?.let { prisonDto -> SupportedPrison(prisonDto.prisonId, true) }
      ?.let { supportedPrison -> supportedPrisonsRepository.save(supportedPrison) }
      ?.let { supportedPrison -> supportedPrison.code }
}
