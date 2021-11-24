package uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.integration.magiclink

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.integration.IntegrationTest
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.magiclink.MagicLinkSecret
import javax.persistence.EntityNotFoundException

class MagicLinkServiceTest : IntegrationTest() {

  @Nested
  inner class VerifyMagicLink {

    @Test
    fun `generates a valid JWT for a magic link secret and deletes the secret`() {
      magicLinkSecretRepository.save(MagicLinkSecret("some-secret", "some.email@company.com.cjsm.net"))

      val jwt = magicLinkService.verifyMagicLinkSecret("some-secret")

      assertThat(magicLinkSecretRepository.findById("some-secret")).isEmpty
      assertThat(jwtService.validateToken(jwt)).isTrue
      assertThat(jwtService.subject(jwt)).isEqualTo("some.email@company.com.cjsm.net")
    }

    @Test
    fun `throws a not found if the secret doesn't exist`() {
      assertThat(magicLinkSecretRepository.findById("some-secret")).isEmpty

      assertThatThrownBy { magicLinkService.verifyMagicLinkSecret("some-secret") }
        .isInstanceOf(EntityNotFoundException::class.java)

      assertThat(magicLinkSecretRepository.findById("some-secret")).isEmpty
    }
  }
}
