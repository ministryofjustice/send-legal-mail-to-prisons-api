package uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.contact

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.IntegrationTest
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.config.AuthenticationError
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.config.DuplicateContact
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.config.MalformedRequest

class ContactResourceTest : IntegrationTest() {

  private companion object {
    val JOHN_SMITH = CreateContactRequest(
      prisonerName = "John Smith",
      prisonId = "BXI",
      prisonNumber = "A1234BC"
    )
  }

  @Nested
  inner class CreateContact {
    @Test
    fun `unauthorised without a valid auth token`() {
      webTestClient.post()
        .uri("/contact")
        .accept(MediaType.APPLICATION_JSON)
        .contentType(MediaType.APPLICATION_JSON)
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `forbidden without a valid role`() {
      webTestClient.post()
        .uri("/contact")
        .accept(MediaType.APPLICATION_JSON)
        .contentType(MediaType.APPLICATION_JSON)
        .headers(setAuthorisation(user = "AUSER_GEN"))
        .bodyValue(JOHN_SMITH)
        .exchange()
        .expectStatus().isForbidden
        .expectBody().jsonPath("$.errorCode.code").isEqualTo(AuthenticationError.code)
    }

    @Test
    fun `bad request given empty body`() {
      webTestClient.post()
        .uri("/contact")
        .accept(MediaType.APPLICATION_JSON)
        .contentType(MediaType.APPLICATION_JSON)
        .headers(setCreateBarcodeAuthorisation())
        .bodyValue("{}")
        .exchange()
        .expectStatus().isBadRequest
        .expectBody().jsonPath("$.errorCode.code").isEqualTo(MalformedRequest.code)
    }

    @Test
    fun `bad request given invalid dob format`() {
      webTestClient.post()
        .uri("/contact")
        .accept(MediaType.APPLICATION_JSON)
        .contentType(MediaType.APPLICATION_JSON)
        .headers(setCreateBarcodeAuthorisation())
        .bodyValue(
          """{ 
            "prisonerName": "John Smith",
            "prisonId": "BXI",
            "dob": "17-08-1971"
          }"""
        )
        .exchange()
        .expectStatus().isBadRequest
        .expectBody().jsonPath("$.errorCode.code").isEqualTo(MalformedRequest.code)
    }

    @Test
    fun `bad request given neither prison number or dob`() {
      webTestClient.post()
        .uri("/contact")
        .accept(MediaType.APPLICATION_JSON)
        .contentType(MediaType.APPLICATION_JSON)
        .headers(setCreateBarcodeAuthorisation())
        .bodyValue(
          """{ 
            "prisonerName": "John Smith",
            "prisonId": "BXI"
          }"""
        )
        .exchange()
        .expectStatus().isBadRequest
        .expectBody().jsonPath("$.errorCode.code").isEqualTo(MalformedRequest.code)
    }

    @Test
    fun `bad request given invalid format prisonNumber`() {
      webTestClient.post()
        .uri("/contact")
        .accept(MediaType.APPLICATION_JSON)
        .contentType(MediaType.APPLICATION_JSON)
        .headers(setCreateBarcodeAuthorisation())
        .bodyValue(
          """{ 
            "prisonerName": "John Smith",
            "prisonId": "BXI",
            "prisonNumber": "not a valid value"
          }"""
        )
        .exchange()
        .expectStatus().isBadRequest
        .expectBody().jsonPath("$.errorCode.code").isEqualTo(MalformedRequest.code)
    }

    @Test
    fun `bad request given invalid format prisonId`() {
      webTestClient.post()
        .uri("/contact")
        .accept(MediaType.APPLICATION_JSON)
        .contentType(MediaType.APPLICATION_JSON)
        .headers(setCreateBarcodeAuthorisation())
        .bodyValue(
          """{ 
            "prisonerName": "John Smith",
            "prisonId": "not a valid value",
            "prisonNumber": "A1234BC"
          }"""
        )
        .exchange()
        .expectStatus().isBadRequest
        .expectBody().jsonPath("$.errorCode.code").isEqualTo(MalformedRequest.code)
    }

    @Test
    fun `new contact is created`() {
      webTestClient.post()
        .uri("/contact")
        .accept(MediaType.APPLICATION_JSON)
        .contentType(MediaType.APPLICATION_JSON)
        .headers(setCreateBarcodeAuthorisation())
        .bodyValue(JOHN_SMITH)
        .exchange()
        .expectStatus().isCreated

      assertThat(contactRepository.findAll()).hasSize(1)
    }

    @Test
    fun `duplicate contacts without prison number are created`() {
      repeat(2) {
        webTestClient.post()
          .uri("/contact")
          .accept(MediaType.APPLICATION_JSON)
          .contentType(MediaType.APPLICATION_JSON)
          .headers(setCreateBarcodeAuthorisation())
          .bodyValue(
            """{ 
            "prisonerName": "John Smith",
            "prisonId": "BXI",
            "dob": "1972-05-29"
          }"""
          )
          .exchange()
          .expectStatus().isCreated
      }

      assertThat(contactRepository.findAll()).hasSize(2)
    }

    @Test
    fun `duplicate contact with prison number is not created`() {
      webTestClient.post()
        .uri("/contact")
        .accept(MediaType.APPLICATION_JSON)
        .contentType(MediaType.APPLICATION_JSON)
        .headers(setCreateBarcodeAuthorisation())
        .bodyValue(JOHN_SMITH)
        .exchange()
        .expectStatus().isCreated

      webTestClient.post()
        .uri("/contact")
        .accept(MediaType.APPLICATION_JSON)
        .contentType(MediaType.APPLICATION_JSON)
        .headers(setCreateBarcodeAuthorisation())
        .bodyValue(JOHN_SMITH)
        .exchange()
        .expectStatus().isEqualTo(HttpStatus.CONFLICT)
        .expectBody().jsonPath("$.errorCode.code").isEqualTo(DuplicateContact.code)

      assertThat(contactRepository.findAll()).hasSize(1)
    }
  }
}
