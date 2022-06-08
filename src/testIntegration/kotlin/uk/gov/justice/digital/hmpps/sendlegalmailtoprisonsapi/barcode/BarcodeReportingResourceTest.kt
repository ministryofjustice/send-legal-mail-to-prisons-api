package uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.barcode

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.given
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.client.WebClient
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.IntegrationTest
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.magiclink.MessageJson

class BarcodeReportingResourceTest(
  @Value("\${mailcatcher.api.port}") private val mailcatcherApiPort: Int,
) : IntegrationTest() {

  private val mailCatcherWebClient = WebClient.builder().baseUrl("http://localhost:$mailcatcherApiPort").build()

  @AfterEach
  fun `clear mail server`() {
    mailCatcherWebClient.delete().uri("/messages").retrieve().bodyToMono(Void::class.java).block()
  }

  @Test
  fun `should send barcode stats report`() {
    webTestClient.post()
      .uri("/barcode-stats-report")
      .exchange()
      .expectStatus().isCreated

    val recipients = getEmailRecipients()
    assertThat(recipients).containsExactlyInAnyOrder("<auser@company.com>", "<anotheruser@anothercompany.com>")
  }

  @Test
  fun `should not send barcode stats report if no recipients`() {
    given { barcodeReportingConfig.recipientEmails }.willReturn(listOf())

    webTestClient.post()
      .uri("/barcode-stats-report")
      .exchange()
      .expectStatus().isNotFound
  }

  private fun getEmailRecipients(): List<String> {
    val messageJson = mailCatcherWebClient.get()
      .uri("/messages/1.json")
      .accept(MediaType.APPLICATION_JSON)
      .retrieve()
      .bodyToMono(MessageJson::class.java)
      .block() as MessageJson
    return messageJson.recipients
  }
}
