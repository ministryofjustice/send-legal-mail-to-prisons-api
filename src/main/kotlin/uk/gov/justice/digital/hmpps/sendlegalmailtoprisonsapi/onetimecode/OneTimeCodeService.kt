package uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.onetimecode

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.cjsm.CjsmService
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.config.ResourceNotFoundException
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.security.JwtService
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.security.SmokeTestConfig

@Service
class OneTimeCodeService(
  private val oneTimeCodeGenerator: OneTimeCodeGenerator,
  private val oneTimeCodeRepository: OneTimeCodeRepository,
  private val oneTimeCodeEmailSender: OneTimeCodeEmailSender,
  private val jwtService: JwtService,
  private val cjsmService: CjsmService,
  private val smokeTestConfig: SmokeTestConfig,
) {

  fun createAndSendOneTimeCode(email: String, sessionId: String) {
    OneTimeCode(sessionId, oneTimeCodeGenerator.generateOneTimeCode(), email)
      .also { oneTimeCode -> oneTimeCodeRepository.save(oneTimeCode) }
      .also { oneTimeCode -> oneTimeCodeEmailSender.send(email, oneTimeCode.code) }
  }

  fun verifyOneTimeCode(code: String, sessionId: String): String =
    if (code == smokeTestConfig.lsjSecret) {
      smokeTest()
    } else {
      oneTimeCodeRepository.findById(sessionId)
        .orElseGet { throw ResourceNotFoundException("One Time Code not found") }
        .takeIf { oneTimeCode -> oneTimeCode.code.equals(code, ignoreCase = true) }
        ?.also { oneTimeCodeRepository.deleteById(sessionId) }
        ?.let { oneTimeCode -> jwtService.generateToken(oneTimeCode.email, findOrganisation(oneTimeCode.email)) }
        ?: throw ResourceNotFoundException("One Time Code not found") // TODO - this is where '3 strikes and out' will be handled when we get to that
    }

  private fun smokeTest(): String = jwtService.generateToken("smoke-test-lsj", "smoke-test-lsj-org")

  private fun findOrganisation(userId: String): String? =
    cjsmService.findOrganisation(userId)
      ?.takeIf { it.isNotBlank() }
}
