package uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.contact

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.IntegrationTest
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.config.AuthenticationError
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.config.MalformedRequest
import java.time.Instant
import java.time.temporal.ChronoUnit

class ContactResourceUpdateContactTest : IntegrationTest() {

  private companion object {
    val JOHN_SMITH = ContactRequest(
      prisonerName = "John Smith",
      prisonId = "BXI",
      prisonNumber = "A1234BC"
    )
  }

  @Test
  fun `unauthorised without a valid auth token`() {
    webTestClient.put()
      .uri("/contact/1")
      .accept(MediaType.APPLICATION_JSON)
      .contentType(MediaType.APPLICATION_JSON)
      .exchange()
      .expectStatus().isUnauthorized
  }

  @Test
  fun `forbidden without a valid role`() {
    webTestClient.put()
      .uri("/contact/1")
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
    webTestClient.put()
      .uri("/contact/1")
      .accept(MediaType.APPLICATION_JSON)
      .contentType(MediaType.APPLICATION_JSON)
      .headers(setCreateBarcodeAuthorisation())
      .bodyValue("{}")
      .exchange()
      .expectStatus().isBadRequest
      .expectBody().jsonPath("$.errorCode.code").isEqualTo(MalformedRequest.code)
  }

  @Test
  fun `method not allowed if no id`() {
    webTestClient.put()
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
      .expectStatus().is4xxClientError
  }

  @Test
  fun `bad request given request body fails javax validation for Date of Birth`() {
    webTestClient.put()
      .uri("/contact/1")
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
  fun `bad request given request body fails RequestBodyValidators validation (neither prison number or dob)`() {
    webTestClient.put()
      .uri("/contact/1")
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
  @Disabled // TODO SLM-147 enable this test once ContactService#updateContact has been implemented
  fun `new contact is updated`() {
    val createdTime = Instant.now().minus(1, ChronoUnit.DAYS)
    val existingContactId = contactRepository.save(
      Contact(
        owner = "some.user@company.com.cjsm.net",
        name = "Johnnie Smith",
        prisonCode = "LEI",
        prisonNumber = "A1111ZZ",
        created = createdTime,
        updated = createdTime
      )
    ).id

    webTestClient.post()
      .uri("/contact/1")
      .accept(MediaType.APPLICATION_JSON)
      .contentType(MediaType.APPLICATION_JSON)
      .headers(setCreateBarcodeAuthorisation())
      .bodyValue(JOHN_SMITH)
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.name").isEqualTo(JOHN_SMITH.prisonerName)
      .jsonPath("$.prisonId").isEqualTo(JOHN_SMITH.prisonId)
      .jsonPath("$.prisonNumber").isEqualTo(JOHN_SMITH.prisonNumber!!)

    val savedContact = contactRepository.getById(existingContactId!!)
    assertThat(savedContact.name).isEqualTo(JOHN_SMITH.prisonerName)
    assertThat(savedContact.prisonNumber).isEqualTo(JOHN_SMITH.prisonNumber)
    assertThat(savedContact.prisonCode).isEqualTo(JOHN_SMITH.prisonId)
    assertThat(savedContact.updated).isAfter(savedContact.created)
  }
}
