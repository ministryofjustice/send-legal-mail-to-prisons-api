package uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.onetimecode

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.given
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.cjsm.CjsmService
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.config.OneTimeCodeException
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.security.JwtService
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.security.SmokeTestConfig
import java.util.Optional

class OneTimeCodeServiceTest {

  private val oneTimeCodeGenerator = mock<OneTimeCodeGenerator>()
  private val oneTimeCodeRepository = mock<OneTimeCodeRepository>()
  private val oneTimeCodeAttemptsRepository = mock<OneTimeCodeAttemptsRepository>()
  private val oneTimeCodeEmailSender = mock<OneTimeCodeEmailSender>()
  private val oneTimeCodeConfig = mock<OneTimeCodeConfig>()
  private val jwtService = mock<JwtService>()
  private val cjsmService = mock<CjsmService>()
  private val smokeTestConfig = mock<SmokeTestConfig>()

  private val oneTimeCodeService = OneTimeCodeService(
    oneTimeCodeGenerator,
    oneTimeCodeRepository,
    oneTimeCodeAttemptsRepository,
    oneTimeCodeEmailSender,
    oneTimeCodeConfig,
    jwtService,
    cjsmService,
    smokeTestConfig,
  )

  @BeforeEach
  fun `mock config`() {
    given { oneTimeCodeConfig.maxAttempts }.willReturn(6)
  }

  @Test
  fun `should create and send one time code`() {
    val email = "someone@somewhere.cjsm.net"
    val sessionId = "12345678"
    val code = "ABCD"

    given { oneTimeCodeGenerator.generateOneTimeCode() }.willReturn(code)

    oneTimeCodeService.createAndSendOneTimeCode(email, sessionId)

    verify(oneTimeCodeRepository).save(OneTimeCode(sessionId, code, email))
    verify(oneTimeCodeEmailSender).send(email, code)
  }

  @Test
  fun `should verify one time code and delete it given it exists in the repository`() {
    val email = "someone@somewhere.cjsm.net"
    val organisation = "Aardvark Lawyers"
    val sessionId = "12345678"
    val code = "ABCD"
    val oneTimeCode = OneTimeCode(sessionId, code, email)
    val oneTimeCodeAttempts = OneTimeCodeAttempts(sessionId, setOf())
    val savedOneTimeCodeAttempts = OneTimeCodeAttempts(sessionId, setOf(code))
    given { oneTimeCodeRepository.findById(any()) }.willReturn(Optional.of(oneTimeCode))
    given { oneTimeCodeAttemptsRepository.findById(any()) }.willReturn(Optional.of(oneTimeCodeAttempts))
    given { oneTimeCodeAttemptsRepository.save(any()) }.willReturn(savedOneTimeCodeAttempts)
    given { cjsmService.findOrganisation(any()) }.willReturn(organisation)
    given { jwtService.generateToken(any(), any()) }.willReturn("a-valid-token")

    val token = oneTimeCodeService.verifyOneTimeCode(code, sessionId)

    assertThat(token).isEqualTo("a-valid-token")
    verify(oneTimeCodeRepository).findById(sessionId)
    verify(oneTimeCodeAttemptsRepository).findById(sessionId)
    verify(oneTimeCodeAttemptsRepository).save(savedOneTimeCodeAttempts)
    verify(oneTimeCodeRepository).deleteById(sessionId)
    verify(cjsmService).findOrganisation(email)
    verify(jwtService).generateToken(email, organisation)
  }

  @Test
  fun `should verify one time code case insensitive and delete it given it exists in the repository`() {
    val email = "someone@somewhere.cjsm.net"
    val organisation = "Aardvark Lawyers"
    val sessionId = "12345678"
    val code = "ABCD"
    val oneTimeCode = OneTimeCode(sessionId, code, email)
    val oneTimeCodeAttempts = OneTimeCodeAttempts(sessionId, setOf())
    val savedOneTimeCodeAttempts = OneTimeCodeAttempts(sessionId, setOf(code))
    given { oneTimeCodeRepository.findById(any()) }.willReturn(Optional.of(oneTimeCode))
    given { oneTimeCodeAttemptsRepository.findById(any()) }.willReturn(Optional.of(oneTimeCodeAttempts))
    given { oneTimeCodeAttemptsRepository.save(any()) }.willReturn(savedOneTimeCodeAttempts)
    given { cjsmService.findOrganisation(any()) }.willReturn(organisation)
    given { jwtService.generateToken(any(), any()) }.willReturn("a-valid-token")

    val token = oneTimeCodeService.verifyOneTimeCode(code.lowercase(), sessionId)

    assertThat(token).isEqualTo("a-valid-token")
    verify(oneTimeCodeRepository).findById(sessionId)
    verify(oneTimeCodeRepository).deleteById(sessionId)
    verify(oneTimeCodeAttemptsRepository).findById(sessionId)
    verify(oneTimeCodeAttemptsRepository).save(savedOneTimeCodeAttempts)
    verify(oneTimeCodeAttemptsRepository).deleteById(sessionId)
    verify(cjsmService).findOrganisation(email)
    verify(jwtService).generateToken(email, organisation)
  }

  @Test
  fun `should not verify one time code but save the attempt given the code does not match`() {
    val email = "someone@somewhere.cjsm.net"
    val sessionId = "12345678"
    val code = "ABCD"
    val oneTimeCode = OneTimeCode(sessionId, code, email)
    val oneTimeCodeAttempts = OneTimeCodeAttempts(sessionId, setOf())
    val savedOneTimeCodeAttempts = OneTimeCodeAttempts(sessionId, setOf("ZYXW"))
    given { oneTimeCodeRepository.findById(any()) }.willReturn(Optional.of(oneTimeCode))
    given { oneTimeCodeAttemptsRepository.findById(any()) }.willReturn(Optional.of(oneTimeCodeAttempts))
    given { oneTimeCodeAttemptsRepository.save(any()) }.willReturn(savedOneTimeCodeAttempts)

    assertThatThrownBy {
      oneTimeCodeService.verifyOneTimeCode("ZYXW", sessionId)
    }.isInstanceOf(OneTimeCodeException::class.java)
      .hasMessage("OTC_NOT_FOUND")

    verify(oneTimeCodeRepository).findById(sessionId)
    verify(oneTimeCodeRepository, never()).deleteById(any())
    verify(oneTimeCodeAttemptsRepository).findById(sessionId)
    verify(oneTimeCodeAttemptsRepository).save(savedOneTimeCodeAttempts)
    verify(oneTimeCodeAttemptsRepository, never()).deleteById(sessionId)
    verifyNoInteractions(cjsmService)
    verifyNoInteractions(jwtService)
  }

  @Test
  fun `should return too many attempts and delete session from one time code repos`() {
    val email = "someone@somewhere.cjsm.net"
    val sessionId = "12345678"
    val code = "ABCD"
    val oneTimeCode = OneTimeCode(sessionId, code, email)
    val oneTimeCodeAttempts = OneTimeCodeAttempts(sessionId, setOf("AAAA", "BBBB", "CCCC", "DDDD", "EEEE"))
    val savedOneTimeCodeAttempts = OneTimeCodeAttempts(sessionId, setOf("AAAA", "BBBB", "CCCC", "DDDD", "EEEE", "ZYXW"))
    given { oneTimeCodeRepository.findById(any()) }.willReturn(Optional.of(oneTimeCode))
    given { oneTimeCodeAttemptsRepository.findById(any()) }.willReturn(Optional.of(oneTimeCodeAttempts))
    given { oneTimeCodeAttemptsRepository.save(any()) }.willReturn(savedOneTimeCodeAttempts)

    assertThatThrownBy {
      oneTimeCodeService.verifyOneTimeCode("ZYXW", sessionId)
    }.isInstanceOf(OneTimeCodeException::class.java)
      .hasMessage("OTC_TOO_MANY_ATTEMPTS")

    verify(oneTimeCodeRepository).findById(sessionId)
    verify(oneTimeCodeRepository).deleteById(any())
    verify(oneTimeCodeAttemptsRepository).findById(sessionId)
    verify(oneTimeCodeAttemptsRepository).save(savedOneTimeCodeAttempts)
    verify(oneTimeCodeAttemptsRepository).deleteById(sessionId)
    verifyNoInteractions(cjsmService)
    verifyNoInteractions(jwtService)
  }

  @Test
  fun `should not verify one time code given it is not in the repository`() {
    val sessionId = "12345678"
    val code = "ABCD"
    given { oneTimeCodeRepository.findById(any()) }.willReturn(Optional.empty())

    assertThatThrownBy {
      oneTimeCodeService.verifyOneTimeCode(code, sessionId)
    }.isInstanceOf(OneTimeCodeException::class.java)
      .hasMessage("OTC_SESSION_NOT_FOUND")

    verify(oneTimeCodeRepository).findById(sessionId)
    verify(oneTimeCodeRepository).deleteById(any())
    verify(oneTimeCodeAttemptsRepository).deleteById(any())
    verifyNoInteractions(cjsmService)
    verifyNoInteractions(jwtService)
  }
}
