package uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.onetimecode

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.cjsm.CjsmService
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.config.OneTimeCodeException
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.config.OneTimeCodeNotFound
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.config.OneTimeCodeSessionNotFound
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.config.OneTimeCodeTooManyAttempts
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.security.JwtService
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.security.SmokeTestConfig
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.toNullable

@Service
class OneTimeCodeService(
  private val oneTimeCodeGenerator: OneTimeCodeGenerator,
  private val oneTimeCodeRepository: OneTimeCodeRepository,
  private val oneTimeCodeAttemptsRepository: OneTimeCodeAttemptsRepository,
  private val oneTimeCodeEmailSender: OneTimeCodeEmailSender,
  private val oneTimeCodeConfig: OneTimeCodeConfig,
  private val jwtService: JwtService,
  private val cjsmService: CjsmService,
  private val smokeTestConfig: SmokeTestConfig,
) {

  fun createAndSendOneTimeCode(email: String, sessionId: String) {
    OneTimeCode(sessionId, oneTimeCodeGenerator.generateOneTimeCode(), email)
      .also { oneTimeCode -> oneTimeCodeRepository.save(oneTimeCode) }
      .also { oneTimeCodeAttemptsRepository.save(OneTimeCodeAttempts(sessionId, emptySet())) }
      .also { oneTimeCode -> oneTimeCodeEmailSender.send(email, oneTimeCode.code) }
  }

  fun verifyOneTimeCode(code: String, sessionId: String): String =
    if (code == smokeTestConfig.lsjSecret) {
      smokeTest()
    } else {

      val oneTimeCode = oneTimeCodeRepository.findById(sessionId).toNullable()
      val attempts = oneTimeCodeAttemptsRepository.findById(sessionId).toNullable()
        ?.let {
          oneTimeCodeAttemptsRepository.save(
            OneTimeCodeAttempts(sessionId, it.attempts?.plus(code.uppercase()) ?: mutableSetOf(code.uppercase()))
          )
        }
      when {
        oneTimeCode == null || attempts == null -> {
          clearOneTimeCode(sessionId)
          throw OneTimeCodeException(OneTimeCodeSessionNotFound)
        }
        oneTimeCode.code.equals(code, ignoreCase = true) -> {
          clearOneTimeCode(sessionId)
          jwtService.generateToken(oneTimeCode.email, findOrganisation(oneTimeCode.email))
        }
        attempts.attempts!!.size >= oneTimeCodeConfig.maxAttempts -> {
          clearOneTimeCode(sessionId)
          throw OneTimeCodeException(OneTimeCodeTooManyAttempts)
        }
        else -> throw OneTimeCodeException(OneTimeCodeNotFound)
      }
    }

  private fun clearOneTimeCode(sessionId: String) {
    oneTimeCodeRepository.deleteById(sessionId)
    oneTimeCodeAttemptsRepository.deleteById(sessionId)
  }

  private fun smokeTest(): String = jwtService.generateToken("smoke-test-lsj", "smoke-test-lsj-org")

  private fun findOrganisation(userId: String): String? =
    cjsmService.findOrganisation(userId)
      ?.takeIf { it.isNotBlank() }
}
