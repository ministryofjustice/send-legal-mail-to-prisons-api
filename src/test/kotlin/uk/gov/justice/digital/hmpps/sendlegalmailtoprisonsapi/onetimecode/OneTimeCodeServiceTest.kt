package uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.onetimecode

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.given
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.cjsm.CjsmService
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.config.ResourceNotFoundException
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.security.JwtService
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.security.SmokeTestConfig
import java.util.Optional

class OneTimeCodeServiceTest {

  private val oneTimeCodeGenerator = mock<OneTimeCodeGenerator>()
  private val oneTimeCodeRepository = mock<OneTimeCodeRepository>()
  private val oneTimeCodeEmailSender = mock<OneTimeCodeEmailSender>()
  private val jwtService = mock<JwtService>()
  private val cjsmService = mock<CjsmService>()
  private val smokeTestConfig = mock<SmokeTestConfig>()

  private val oneTimeCodeService = OneTimeCodeService(
    oneTimeCodeGenerator, oneTimeCodeRepository, oneTimeCodeEmailSender, jwtService, cjsmService, smokeTestConfig
  )

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
    given { oneTimeCodeRepository.findById(any()) }.willReturn(Optional.of(oneTimeCode))
    given { cjsmService.findOrganisation(any()) }.willReturn(organisation)
    given { jwtService.generateToken(any(), any()) }.willReturn("a-valid-token")

    val token = oneTimeCodeService.verifyOneTimeCode(code, sessionId)

    assertThat(token).isEqualTo("a-valid-token")
    verify(oneTimeCodeRepository).findById(sessionId)
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
    given { oneTimeCodeRepository.findById(any()) }.willReturn(Optional.of(oneTimeCode))
    given { cjsmService.findOrganisation(any()) }.willReturn(organisation)
    given { jwtService.generateToken(any(), any()) }.willReturn("a-valid-token")

    val token = oneTimeCodeService.verifyOneTimeCode(code.lowercase(), sessionId)

    assertThat(token).isEqualTo("a-valid-token")
    verify(oneTimeCodeRepository).findById(sessionId)
    verify(oneTimeCodeRepository).deleteById(sessionId)
    verify(cjsmService).findOrganisation(email)
    verify(jwtService).generateToken(email, organisation)
  }

  // The behaviour that this test asserts is vital to supporting a '3 strikes' and out approach as and when we get there
  // IE. not to delete the OneTimeCode from the repository
  @Test
  @Disabled("Implementation does not currently support this as it presents a security risk - SLM-233 should address this")
  fun `should not verify one time code and not delete from the repository given the code does not match`() {
    val email = "someone@somewhere.cjsm.net"
    val sessionId = "12345678"
    val code = "ABCD"
    val oneTimeCode = OneTimeCode(sessionId, code, email)
    given { oneTimeCodeRepository.findById(any()) }.willReturn(Optional.of(oneTimeCode))

    assertThatThrownBy {
      oneTimeCodeService.verifyOneTimeCode("ZYXW", sessionId)
    }.isInstanceOf(ResourceNotFoundException::class.java)
      .hasMessage("One Time Code not found")

    verify(oneTimeCodeRepository).findById(sessionId)
    verify(oneTimeCodeRepository, never()).deleteById(any())
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
    }.isInstanceOf(ResourceNotFoundException::class.java)
      .hasMessage("One Time Code not found")

    verify(oneTimeCodeRepository).findById(sessionId)
    verify(oneTimeCodeRepository, never()).deleteById(any())
    verifyNoInteractions(cjsmService)
    verifyNoInteractions(jwtService)
  }
}
