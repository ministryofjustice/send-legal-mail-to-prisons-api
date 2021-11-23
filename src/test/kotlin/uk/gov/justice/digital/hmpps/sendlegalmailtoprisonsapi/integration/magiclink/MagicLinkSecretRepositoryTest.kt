package uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.integration.magiclink

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.integration.IntegrationTest
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.magiclink.MagicLinkSecret

class MagicLinkSecretRepositoryTest : IntegrationTest() {

  @Test
  fun `can create and retrieve a magic link secret`() {
    val secret = magicLinkSecretRepository.save(MagicLinkSecret(secretValue = "some-secret", email = "someone@somewhere.com"))

    val savedSecret = magicLinkSecretRepository.findById("some-secret").orElseThrow()

    assertThat(savedSecret).isEqualTo(secret)
  }
}
