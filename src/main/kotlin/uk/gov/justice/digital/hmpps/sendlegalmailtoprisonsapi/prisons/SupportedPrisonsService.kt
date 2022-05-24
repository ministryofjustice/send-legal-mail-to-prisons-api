package uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.prisons

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.client.PrisonRegisterClient
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.toNullable

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

  fun removePrisonCode(prisonCode: String): String? =
    supportedPrisonsRepository.findById(prisonCode).toNullable()
      ?.let { supportedPrison -> SupportedPrison(code = supportedPrison.code, active = false) }
      ?.also { inactivePrison -> supportedPrisonsRepository.save(inactivePrison) }
      ?.let { inactivePrison -> inactivePrison.code }
}
