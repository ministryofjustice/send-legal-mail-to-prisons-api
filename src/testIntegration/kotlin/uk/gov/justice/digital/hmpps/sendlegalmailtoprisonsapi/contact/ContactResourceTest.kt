package uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.contact

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.context.jdbc.Sql
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.IntegrationTest
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.config.AuthenticationError
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.config.DuplicateContact
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.config.MalformedRequest
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.config.NotFound

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
    fun `bad request given invalid format name`() {
      webTestClient.post()
        .uri("/contact")
        .accept(MediaType.APPLICATION_JSON)
        .contentType(MediaType.APPLICATION_JSON)
        .headers(setCreateBarcodeAuthorisation())
        .bodyValue(
          """{ 
            "prisonerName": "<John Smith>",
            "prisonId": "BXI",
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

  @Nested
  @Sql("/createContacts.sql")
  inner class SearchContactsByName {
    @Test
    fun `unauthorised without a valid auth token`() {
      webTestClient.get()
        .uri("/contacts?name=fred")
        .accept(MediaType.APPLICATION_JSON)
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `forbidden without a valid role`() {
      webTestClient.get()
        .uri("/contacts?name=fred")
        .accept(MediaType.APPLICATION_JSON)
        .headers(setAuthorisation(user = "AUSER_GEN"))
        .exchange()
        .expectStatus().isForbidden
        .expectBody().jsonPath("$.errorCode.code").isEqualTo(AuthenticationError.code)
    }

    @Test
    fun `bad request without required query string parameter`() {
      webTestClient.get()
        .uri("/contacts")
        .accept(MediaType.APPLICATION_JSON)
        .headers(setAuthorisation())
        .exchange()
        .expectStatus().isBadRequest
        .expectBody().jsonPath("$.errorCode.code").isEqualTo(MalformedRequest.code)
    }

    @Test
    fun `returns matching Contacts given owner and name that match records`() {
      val contacts: List<ContactResponse> = webTestClient.get()
        .uri("/contacts?name=john")
        .accept(MediaType.APPLICATION_JSON)
        .headers(setCreateBarcodeAuthorisation(email = "some.user@company.com.cjsm.net"))
        .exchange()
        .expectStatus().isOk
        .expectBodyList(ContactResponse::class.java).hasSize(4).returnResult().responseBody

      val contactNames = contacts.map { it.prisonerName }
      assertThat(contactNames).containsExactly("John Smith", "Johnathon Evans", "Malcolm Johnston", "Bart Johnson")
    }

    @Test
    fun `returns zero matching Contacts given owner and name that match no records`() {
      webTestClient.get()
        .uri("/contacts?name=john")
        .accept(MediaType.APPLICATION_JSON)
        .headers(setCreateBarcodeAuthorisation(email = "some.user.with.no.contacts.in.the.database@company.com.cjsm.net"))
        .exchange()
        .expectStatus().isOk
        .expectBodyList(ContactResponse::class.java).hasSize(0)
    }
  }

  @Nested
  @Sql("/createContacts.sql")
  inner class GetContactByPrisonNumber {
    @Test
    fun `unauthorised without a valid auth token`() {
      webTestClient.get()
        .uri("/contact/A1234BC")
        .accept(MediaType.APPLICATION_JSON)
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `forbidden without a valid role`() {
      webTestClient.get()
        .uri("/contact/A1234BC")
        .accept(MediaType.APPLICATION_JSON)
        .headers(setAuthorisation(user = "AUSER_GEN"))
        .exchange()
        .expectStatus().isForbidden
        .expectBody().jsonPath("$.errorCode.code").isEqualTo(AuthenticationError.code)
    }

    @Test
    fun `not found given unknown prison number`() {
      webTestClient.get()
        .uri("/contact/X9999ZZ")
        .accept(MediaType.APPLICATION_JSON)
        .headers(setCreateBarcodeAuthorisation())
        .exchange()
        .expectStatus().isNotFound
        .expectBody().jsonPath("$.errorCode.code").isEqualTo(NotFound.code)
    }

    @Test
    fun `returns contact given valid prison number`() {
      webTestClient.get()
        .uri("/contact/A1234BC")
        .accept(MediaType.APPLICATION_JSON)
        .headers(setCreateBarcodeAuthorisation())
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.prisonNumber").isEqualTo("A1234BC")
        .jsonPath("$.prisonerName").isEqualTo("John Smith")
    }
  }
}
