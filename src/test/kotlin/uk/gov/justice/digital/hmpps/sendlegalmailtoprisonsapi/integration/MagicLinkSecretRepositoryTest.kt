package uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.jpa.MagicLinkSecret

class MagicLinkSecretRepositoryTest : IntegrationTest() {

  @Test
  fun `can create and retrieve a magic link secret`() {
    val secret = magicLinkSecretRepository.save(MagicLinkSecret(email = "someone@somewhere.com", secretValue = "some-secret"))

    val savedSecret = magicLinkSecretRepository.findById("someone@somewhere.com").orElseThrow()

    assertThat(savedSecret).isEqualTo(secret)
  }
}
