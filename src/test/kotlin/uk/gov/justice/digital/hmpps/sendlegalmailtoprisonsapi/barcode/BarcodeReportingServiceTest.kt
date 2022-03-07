package uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.barcode

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.check
import org.mockito.kotlin.given
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.then
import org.springframework.mail.javamail.JavaMailSender
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.config.ResourceNotFoundException
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.mail.Session
import javax.mail.internet.MimeMessage
import javax.mail.internet.MimeMultipart

class BarcodeReportingServiceTest {

  private val barcodeStatsService = mock<BarcodeStatsService>()
  private val javaMailSender = mock<JavaMailSender>()
  private val barcodeReportingConfig = mock<BarcodeReportingConfig>()
  private val barcodeReportingService = BarcodeReportingService(barcodeStatsService, javaMailSender, barcodeReportingConfig)

  private val reportDate = LocalDate.now().minusDays(1)

  @BeforeEach
  fun `stub stats`() {
    given(barcodeStatsService.countBarcodesCreated()).willReturn(1)
    given(barcodeStatsService.countBarcodesScanned()).willReturn(2)
    given(barcodeStatsService.countBarcodesCreatedOnDay(any())).willReturn(3)
    given(barcodeStatsService.countBarcodesScannedOnDay(any())).willReturn(4)
    given(barcodeStatsService.countUniqueUsersCreatedBarcodes()).willReturn(5)
  }

  @BeforeEach
  fun `stub create MimeMessage`() {
    given(javaMailSender.createMimeMessage()).willReturn(MimeMessage(null as Session?))
  }

  @BeforeEach
  fun `stub email recipients`() {
    given(barcodeReportingConfig.recipientEmails).willReturn(listOf("email1", "email2"))
  }

  @Test
  fun `should throw if no email recipients configured`() {
    given(barcodeReportingConfig.recipientEmails).willReturn(listOf())

    assertThatThrownBy {
      barcodeReportingService.distributeBarcodeStats()
    }.isInstanceOf(ResourceNotFoundException::class.java)

    then(barcodeStatsService).should(never()).countBarcodesCreated()
    then(javaMailSender).should(never()).createMimeMessage()
  }

  @Test
  fun `should call the barcode stats server`() {
    barcodeReportingService.distributeBarcodeStats()

    then(barcodeStatsService).should().countBarcodesCreated()
    then(barcodeStatsService).should().countBarcodesScanned()
    then(barcodeStatsService).should().countBarcodesCreatedOnDay(reportDate)
    then(barcodeStatsService).should().countBarcodesScannedOnDay(reportDate)
    then(barcodeStatsService).should().countUniqueUsersCreatedBarcodes()
  }

  @Test
  fun `should send an email to all configured users`() {
    given(barcodeReportingConfig.recipientEmails).willReturn(listOf("email1", "email2"))

    barcodeReportingService.distributeBarcodeStats()

    then(javaMailSender).should().send(
      check<MimeMessage> { message ->
        assertThat(message.allRecipients.map { it.toString() }).containsExactlyInAnyOrder("email1", "email2")
      }
    )
  }

  @Test
  fun `should include report date in the subject`() {
    val expectedDate = DateTimeFormatter.ofPattern("YYYY-MM-DD").format(reportDate)

    barcodeReportingService.distributeBarcodeStats()

    then(javaMailSender).should().send(
      check<MimeMessage> { message ->
        assertThat(message.subject).contains(expectedDate)
      }
    )
  }

  @Test
  fun `should include stats in email body`() {
    val expectedDate = DateTimeFormatter.ofPattern("YYYY-MM-DD").format(reportDate)

    barcodeReportingService.distributeBarcodeStats()

    then(javaMailSender).should().send(
      check<MimeMessage> { message ->
        val body = (message.content as MimeMultipart).getBodyPartContents()["text/plain"]
        assertThat(body).contains(": 1")
        assertThat(body).contains(": 2")
        assertThat(body).contains("on $expectedDate: 3")
        assertThat(body).contains("on $expectedDate: 4")
        assertThat(body).contains(": 5")
      }
    )
  }

  private fun MimeMultipart.getBodyPartContents(): Map<String, String> {
    val bodyPartContents: MutableMap<String, String> = mutableMapOf()
    for (idx in 0 until this.count) {
      val bodyPart = this.getBodyPart(idx)
      if (bodyPart.content is MimeMultipart) {
        bodyPartContents.putAll((bodyPart.content as MimeMultipart).getBodyPartContents())
      } else {
        bodyPartContents[bodyPart.dataHandler.contentType.substringBefore(";")] = bodyPart.dataHandler.content.toString()
      }
    }
    return bodyPartContents.toMap()
  }
}
