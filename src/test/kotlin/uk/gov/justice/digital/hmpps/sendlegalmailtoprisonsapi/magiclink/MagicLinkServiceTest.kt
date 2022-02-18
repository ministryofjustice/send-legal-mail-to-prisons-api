package uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.magiclink

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.given
import org.mockito.kotlin.mock
import org.mockito.kotlin.then
import org.mockito.kotlin.verifyNoInteractions
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.cjsm.CjsmService
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.config.ResourceNotFoundException
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.security.JwtService
import java.util.Optional
import java.util.UUID

class MagicLinkServiceTest {

  private val magicLinkEmailSender = mock<MagicLinkEmailSender>()
  private val magicLinkSecretRepository = mock<MagicLinkSecretRepository>()
  private val jwtService = mock<JwtService>()
  private val cjsmService = mock<CjsmService>()
  private val magicLinkService = MagicLinkService(magicLinkEmailSender, magicLinkSecretRepository, jwtService, cjsmService)

  @Nested
  inner class CreateAndSendMagicLink {
    @Test
    fun `should create and send magic link`() {
      val email = "fred@cjsm.net"

      magicLinkService.createAndSendMagicLink(email)

      val argumentCaptor = argumentCaptor<MagicLinkSecret>()
      then(magicLinkSecretRepository).should().save(argumentCaptor.capture())
      val savedMagicSecret = argumentCaptor.firstValue
      then(magicLinkEmailSender).should().send(email, savedMagicSecret.secretValue)
    }
  }

  @Nested
  inner class VerifyMagicLinkSecret {
    @Test
    fun `should verify magic link secret`() {
      val email = "fred@cjsm.net"
      val secret = UUID.randomUUID().toString()
      val expectedJwtToken = "a jwt token"

      val magicLinkSecret = MagicLinkSecret(secret, email)
      given { magicLinkSecretRepository.findById(secret) }.willReturn(Optional.of(magicLinkSecret))
      given { cjsmService.findOrganisation(email) }.willReturn("Some Organisation")
      given { jwtService.generateToken(any(), any()) }.willReturn(expectedJwtToken)

      val jwt = magicLinkService.verifyMagicLinkSecret(secret)

      assertThat(jwt).isEqualTo(expectedJwtToken)
      then(magicLinkSecretRepository).should().deleteById(secret)
      then(jwtService).should().generateToken("fred@cjsm.net", "Some Organisation")
    }

    @Test
    fun `should verify magic link secret given organisation is not resolved`() {
      val email = "fred@cjsm.net"
      val secret = UUID.randomUUID().toString()
      val expectedJwtToken = "a jwt token"

      val magicLinkSecret = MagicLinkSecret(secret, email)
      given { magicLinkSecretRepository.findById(secret) }.willReturn(Optional.of(magicLinkSecret))
      given { cjsmService.findOrganisation(email) }.willReturn(null)
      given { jwtService.generateToken(any(), anyOrNull()) }.willReturn(expectedJwtToken)

      val jwt = magicLinkService.verifyMagicLinkSecret(secret)

      assertThat(jwt).isEqualTo(expectedJwtToken)
      then(magicLinkSecretRepository).should().deleteById(secret)
      then(jwtService).should().generateToken("fred@cjsm.net", null)
    }

    @Test
    fun `should throw exception given secret not found in database`() {
      val secret = UUID.randomUUID().toString()
      given { magicLinkSecretRepository.findById(secret) }.willReturn(Optional.empty())

      assertThatExceptionOfType(ResourceNotFoundException::class.java).isThrownBy {
        magicLinkService.verifyMagicLinkSecret(secret)
      }

      verifyNoInteractions(jwtService)
      verifyNoInteractions(cjsmService)
    }
  }
}
