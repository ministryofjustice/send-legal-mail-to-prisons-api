package uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.onetimecode

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.fail
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.mockito.kotlin.eq
import org.mockito.kotlin.times
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.mock.mockito.SpyBean
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.BodyInserters
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.IntegrationTest
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.config.EmailInvalid
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.config.EmailInvalidCjsm
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.config.EmailMandatory
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.config.MalformedRequest
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.config.SessionIdMandatory
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.notifications.NotificationService

class OneTimeCodeResourceTest(
  @Value("\${app.notify.template-id.one-time-code-email}") private val oneTimeCodeEmailTemplateId: String,
) : IntegrationTest() {

  @SpyBean
  lateinit var notificationServiceSpy: NotificationService

  @Nested
  inner class CreateOneTimeCode {
    @Test
    fun `unauthorised without a valid auth token`() {
      webTestClient.post()
        .uri("/oneTimeCode/email")
        .accept(MediaType.APPLICATION_JSON)
        .contentType(MediaType.APPLICATION_JSON)
        .body(BodyInserters.fromValue("""{ "email": "some.email@company.com.cjsm.net", "sessionID": "12345678" }"""))
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `bad request without body`() {
      webTestClient.post()
        .uri("/oneTimeCode/email")
        .accept(MediaType.APPLICATION_JSON)
        .contentType(MediaType.APPLICATION_JSON)
        .headers(setAuthorisation(user = null))
        .exchange()
        .expectStatus().isBadRequest
        .expectBody().jsonPath("$.errorCode.code").isEqualTo(MalformedRequest.code)
    }

    @Test
    fun `bad request with malformed body`() {
      webTestClient.post()
        .uri("/oneTimeCode/email")
        .accept(MediaType.APPLICATION_JSON)
        .contentType(MediaType.APPLICATION_JSON)
        .headers(setAuthorisation(user = null))
        .body(BodyInserters.fromValue("""{ "didnt-expect-this": "some.email@company.com.cjsm.net" }"""))
        .exchange()
        .expectStatus().isBadRequest
        .expectBody().jsonPath("$.errorCode.code").isEqualTo(MalformedRequest.code)
    }

    @Test
    fun `bad request with missing email`() {
      webTestClient.post()
        .uri("/oneTimeCode/email")
        .accept(MediaType.APPLICATION_JSON)
        .contentType(MediaType.APPLICATION_JSON)
        .headers(setAuthorisation(user = null))
        .body(BodyInserters.fromValue("""{ "email": "", "sessionID": "12345678" }"""))
        .exchange()
        .expectStatus().isBadRequest
        .expectBody()
        .jsonPath("$.errorCode.code").isEqualTo(EmailMandatory.code)
        .jsonPath("$.errorCode.userMessage").value<String> { it.contains("email address") }
    }

    @Test
    fun `bad request with invalid email`() {
      webTestClient.post()
        .uri("/oneTimeCode/email")
        .accept(MediaType.APPLICATION_JSON)
        .contentType(MediaType.APPLICATION_JSON)
        .headers(setAuthorisation(user = null))
        .body(BodyInserters.fromValue("""{ "email": "invalid@email", "sessionID": "12345678" }"""))
        .exchange()
        .expectStatus().isBadRequest
        .expectBody()
        .jsonPath("$.errorCode.code").isEqualTo(EmailInvalid.code)
        .jsonPath("$.errorCode.userMessage").value<String> { it.contains("email address") }
    }

    @Test
    fun `bad request with non CJSM email`() {
      webTestClient.post()
        .uri("/oneTimeCode/email")
        .accept(MediaType.APPLICATION_JSON)
        .contentType(MediaType.APPLICATION_JSON)
        .headers(setAuthorisation(user = null))
        .body(BodyInserters.fromValue("""{ "email": "some.email@company.com", "sessionID": "12345678" }"""))
        .exchange()
        .expectStatus().isBadRequest
        .expectBody()
        .jsonPath("$.errorCode.code").isEqualTo(EmailInvalidCjsm.code)
        .jsonPath("$.errorCode.userMessage").value<String> { it.contains("cjsm.net") }
    }

    @Test
    fun `bad request with empty sessionId`() {
      webTestClient.post()
        .uri("/oneTimeCode/email")
        .accept(MediaType.APPLICATION_JSON)
        .contentType(MediaType.APPLICATION_JSON)
        .headers(setAuthorisation(user = null))
        .body(BodyInserters.fromValue("""{ "email": "some.email@company.com.cjsm.net", "sessionID": "" }"""))
        .exchange()
        .expectStatus().isBadRequest
        .expectBody()
        .jsonPath("$.errorCode.code").isEqualTo(SessionIdMandatory.code)
        .jsonPath("$.errorCode.userMessage").value<String> { it.contains("session ID") }
    }

    @Test
    fun `saves one time code and sends email`() {
      webTestClient.post()
        .uri("/oneTimeCode/email")
        .accept(MediaType.APPLICATION_JSON)
        .contentType(MediaType.APPLICATION_JSON)
        .headers(setAuthorisation(user = null))
        .body(BodyInserters.fromValue("""{ "email": "some.email@company.com.cjsm.net", "sessionID": "12345678" }"""))
        .exchange()
        .expectStatus().isCreated

      val savedOneTimeCode = oneTimeCodeRepository.findAll().firstOrNull()
      val values = hashMapOf(Pair("code", savedOneTimeCode!!.code))
      Mockito.verify(notificationServiceSpy, times(1)).sendEmail(eq(oneTimeCodeEmailTemplateId), eq("some.email@company.com.cjsm.net"), eq(values))
    }
  }

  @Nested
  inner class VerifyOneTimeCode {

    @Test
    fun `unauthorised without a valid auth token`() {
      webTestClient.post()
        .uri("/oneTimeCode/verify")
        .accept(MediaType.APPLICATION_JSON)
        .contentType(MediaType.APPLICATION_JSON)
        .body(BodyInserters.fromValue("""{ "code": "ABCD", "sessionID": "12345678" }"""))
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `bad request without body`() {
      webTestClient.post()
        .uri("/oneTimeCode/verify")
        .accept(MediaType.APPLICATION_JSON)
        .contentType(MediaType.APPLICATION_JSON)
        .headers(setAuthorisation())
        .exchange()
        .expectStatus().isBadRequest
        .expectBody().jsonPath("$.errorCode.code").isEqualTo(MalformedRequest.code)
    }

    @Test
    fun `bad request with malformed body`() {
      webTestClient.post()
        .uri("/oneTimeCode/verify")
        .accept(MediaType.APPLICATION_JSON)
        .contentType(MediaType.APPLICATION_JSON)
        .headers(setAuthorisation(user = null))
        .body(BodyInserters.fromValue("""{ "didnt-expect-this": "some-secret-value" }"""))
        .exchange()
        .expectStatus().isBadRequest
        .expectBody().jsonPath("$.errorCode.code").isEqualTo(MalformedRequest.code)
    }

    @Test
    fun `generates a valid JWT for a one time code and deletes the one time code`() {
      oneTimeCodeRepository.save(OneTimeCode("12345678", "ABCD", "some.email@company.com.cjsm.net"))
      oneTimeCodeAttemptsRepository.save(OneTimeCodeAttempts("12345678", setOf()))

      val jwt = webTestClient.post()
        .uri("/oneTimeCode/verify")
        .accept(MediaType.APPLICATION_JSON)
        .contentType(MediaType.APPLICATION_JSON)
        .headers(setAuthorisation(user = null))
        .body(BodyInserters.fromValue("""{ "code": "ABCD", "sessionID": "12345678" }"""))
        .exchange()
        .expectStatus().isCreated
        .returnResult(VerifyCodeResponse::class.java)
        .responseBody
        .blockFirst()
        ?: fail("Did not receive a response from /oneTimeCode/verify")

      assertThat(oneTimeCodeRepository.findById("12345678")).isEmpty
      assertThat(jwtService.validateToken(jwt.token)).isTrue
      assertThat(jwtService.subject(jwt.token)).isEqualTo("some.email@company.com.cjsm.net")
      assertThat(jwtService.authorities(jwt.token)).containsExactly("ROLE_SLM_CREATE_BARCODE")
    }

    @Test
    fun `unauthorised if the one time code doesn't exist`() {
      assertThat(oneTimeCodeRepository.findById("12345678")).isEmpty

      webTestClient.post()
        .uri("/oneTimeCode/verify")
        .accept(MediaType.APPLICATION_JSON)
        .contentType(MediaType.APPLICATION_JSON)
        .headers(setAuthorisation(user = null))
        .body(BodyInserters.fromValue("""{ "code": "XXXX", "sessionID": "12345678" }"""))
        .exchange()
        .expectStatus().isUnauthorized

      assertThat(oneTimeCodeRepository.findById("12345678")).isEmpty
    }
  }
}
