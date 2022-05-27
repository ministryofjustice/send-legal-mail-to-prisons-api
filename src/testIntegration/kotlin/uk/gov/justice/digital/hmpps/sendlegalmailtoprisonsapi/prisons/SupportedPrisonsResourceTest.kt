package uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.prisons

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.IntegrationTest
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.config.AuthenticationError
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.config.NotFound
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.mocks.PrisonRegisterExtension
import java.time.Instant
import java.time.temporal.ChronoUnit

class SupportedPrisonsResourceTest : IntegrationTest() {

  @Nested
  inner class GetSupportedPrison {

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
  }

  @Nested
  inner class AddSupportedPrison {
    @Test
    fun `unauthorised without a valid token`() {
      webTestClient.post()
        .uri("/prisons/some-prison")
        .accept(MediaType.APPLICATION_JSON)
        .contentType(MediaType.APPLICATION_JSON)
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `forbidden without a valid role`() {
      webTestClient.post()
        .uri("/prisons/some-prison")
        .accept(MediaType.APPLICATION_JSON)
        .contentType(MediaType.APPLICATION_JSON)
        .headers(setAuthorisation(user = "AUSER_GEN"))
        .exchange()
        .expectStatus().isForbidden
        .expectBody().jsonPath("$.errorCode.code").isEqualTo(AuthenticationError.code)
    }

    @Test
    fun `not found if prison not in prison register`() {
      PrisonRegisterExtension.prisonRegisterApi.stubGetPrisonNotFound("ABC")

      webTestClient.post()
        .uri("/prisons/ABC")
        .accept(MediaType.APPLICATION_JSON)
        .contentType(MediaType.APPLICATION_JSON)
        .headers(setAuthorisation(user = "AUSER_GEN", roles = listOf("ROLE_SLM_ADMIN")))
        .exchange()
        .expectStatus().isNotFound
        .expectBody().jsonPath("$.errorCode.code").isEqualTo(NotFound.code)
    }

    @Test
    fun `created if the prison has been added`() {
      PrisonRegisterExtension.prisonRegisterApi.stubGetPrisonOk("ABC")

      webTestClient.post()
        .uri("/prisons/ABC")
        .accept(MediaType.APPLICATION_JSON)
        .contentType(MediaType.APPLICATION_JSON)
        .headers(setAuthorisation(user = "SOME_USER", roles = listOf("ROLE_SLM_ADMIN")))
        .exchange()
        .expectStatus().isCreated

      val supportedPrison = supportedPrisonsRepository.getById("ABC")

      assertThat(supportedPrison)
        .extracting(SupportedPrison::code, SupportedPrison::active, SupportedPrison::updatedBy)
        .isEqualTo(listOf("ABC", true, "SOME_USER"))
      assertThat(supportedPrison.updated).isCloseTo(Instant.now(), within(5, ChronoUnit.SECONDS))
    }
    @Test
    fun `created if the prison was already active`() {
      supportedPrisonsRepository.save(
        aSupportedPrison(code = "ABC", active = true, updated = Instant.now().minus(1, ChronoUnit.DAYS))
      )
      PrisonRegisterExtension.prisonRegisterApi.stubGetPrisonOk("ABC")

      webTestClient.post()
        .uri("/prisons/ABC")
        .accept(MediaType.APPLICATION_JSON)
        .contentType(MediaType.APPLICATION_JSON)
        .headers(setAuthorisation(user = "SOME_USER", roles = listOf("ROLE_SLM_ADMIN")))
        .exchange()
        .expectStatus().isCreated

      val supportedPrison = supportedPrisonsRepository.getById("ABC")

      assertThat(supportedPrison)
        .extracting(SupportedPrison::code, SupportedPrison::active, SupportedPrison::updatedBy)
        .isEqualTo(listOf("ABC", true, "SOME_USER"))
      assertThat(supportedPrison.updated).isCloseTo(Instant.now(), within(5, ChronoUnit.SECONDS))
    }

    @Test
    fun `created if the prison has been reactivated`() {
      supportedPrisonsRepository.save(
        aSupportedPrison(code = "CDE", active = false, updated = Instant.now().minus(1, ChronoUnit.DAYS))
      )
      PrisonRegisterExtension.prisonRegisterApi.stubGetPrisonOk("CDE")

      webTestClient.post()
        .uri("/prisons/CDE")
        .accept(MediaType.APPLICATION_JSON)
        .contentType(MediaType.APPLICATION_JSON)
        .headers(setAuthorisation(user = "SOME_USER", roles = listOf("ROLE_SLM_ADMIN")))
        .exchange()
        .expectStatus().isCreated

      val supportedPrison = supportedPrisonsRepository.getById("CDE")

      assertThat(supportedPrison)
        .extracting(SupportedPrison::code, SupportedPrison::active, SupportedPrison::updatedBy)
        .isEqualTo(listOf("CDE", true, "SOME_USER"))
      assertThat(supportedPrison.updated).isCloseTo(Instant.now(), within(5, ChronoUnit.SECONDS))
    }
  }

  @Nested
  inner class RemoveSupportedPrison {
    @Test
    fun `unauthorised without a valid token`() {
      webTestClient.delete()
        .uri("/prisons/some-prison")
        .accept(MediaType.APPLICATION_JSON)
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `forbidden without a valid role`() {
      webTestClient.delete()
        .uri("/prisons/some-prison")
        .accept(MediaType.APPLICATION_JSON)
        .headers(setAuthorisation(user = "AUSER_GEN"))
        .exchange()
        .expectStatus().isForbidden
        .expectBody().jsonPath("$.errorCode.code").isEqualTo(AuthenticationError.code)
    }

    @Test
    fun `not found if prison not in our database`() {
      webTestClient.delete()
        .uri("/prisons/ABC")
        .accept(MediaType.APPLICATION_JSON)
        .headers(setAuthorisation(user = "AUSER_GEN", roles = listOf("ROLE_SLM_ADMIN")))
        .exchange()
        .expectStatus().isNotFound
        .expectBody().jsonPath("$.errorCode.code").isEqualTo(NotFound.code)
    }

    @Test
    fun `ok if the prison has been removed`() {
      supportedPrisonsRepository.save(aSupportedPrison(code = "ABC", active = true, updated = Instant.now().minus(1, ChronoUnit.DAYS)))

      webTestClient.delete()
        .uri("/prisons/ABC")
        .accept(MediaType.APPLICATION_JSON)
        .headers(setAuthorisation(user = "SOME_USER", roles = listOf("ROLE_SLM_ADMIN")))
        .exchange()
        .expectStatus().isOk

      val supportedPrison = supportedPrisonsRepository.getById("ABC")
      assertThat(supportedPrison)
        .extracting(SupportedPrison::code, SupportedPrison::active, SupportedPrison::updatedBy)
        .isEqualTo(listOf("ABC", false, "SOME_USER"))
      assertThat(supportedPrison.updated).isCloseTo(Instant.now(), within(5, ChronoUnit.SECONDS))
    }

    @Test
    fun `ok if the prison was already inactive`() {
      supportedPrisonsRepository.save(
        aSupportedPrison(code = "CDE", active = false, updated = Instant.now().minus(1, ChronoUnit.DAYS))
      )

      webTestClient.delete()
        .uri("/prisons/CDE")
        .accept(MediaType.APPLICATION_JSON)
        .headers(setAuthorisation(user = "SOME_USER", roles = listOf("ROLE_SLM_ADMIN")))
        .exchange()
        .expectStatus().isOk

      val supportedPrison = supportedPrisonsRepository.getById("CDE")
      assertThat(supportedPrison)
        .extracting(SupportedPrison::code, SupportedPrison::active, SupportedPrison::updatedBy)
        .isEqualTo(listOf("CDE", false, "SOME_USER"))
      assertThat(supportedPrison.updated).isCloseTo(Instant.now(), within(5, ChronoUnit.SECONDS))
    }
  }

  private fun aSupportedPrison(code: String, active: Boolean, updated: Instant = Instant.now()): SupportedPrison =
    SupportedPrison(code = code, active = active, updatedBy = "anyone", updated = updated)
}
