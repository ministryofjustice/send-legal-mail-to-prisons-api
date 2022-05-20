package uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.prisons

import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.IntegrationTest
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.config.AuthenticationError
import java.time.Instant

class SupportedPrisonsResourceTest : IntegrationTest() {

  @Test
  fun `unauthorised without a valid token`() {
    webTestClient.get()
      .uri("/prisons")
      .accept(MediaType.APPLICATION_JSON)
      .exchange()
      .expectStatus().isUnauthorized
  }

  @Test
  fun `forbidden without a valid role`() {
    webTestClient.get()
      .uri("/prisons")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(user = "AUSER_GEN"))
      .exchange()
      .expectStatus().isForbidden
      .expectBody().jsonPath("$.errorCode.code").isEqualTo(AuthenticationError.code)
  }

  @Test
  fun `returns supported prisons`() {
    supportedPrisonsRepository.save(aSupportedPrison(code = "CCC", active = true))
    supportedPrisonsRepository.save(aSupportedPrison(code = "BBB", active = false))
    supportedPrisonsRepository.save(aSupportedPrison(code = "AAA", active = true))

    webTestClient.get()
      .uri("/prisons")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(user = "AUSER_GEN", roles = listOf("ROLE_SLM_ADMIN")))
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.supportedPrisons[0]").isEqualTo("AAA")
      .jsonPath("$.supportedPrisons[1]").isEqualTo("CCC")
  }

  private fun aSupportedPrison(code: String, active: Boolean): SupportedPrison =
    SupportedPrison(code = code, active = active, updatedBy = "anyone", updated = Instant.now())
}
