package uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.barcode

import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.MimeMessageHelper
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.config.ResourceNotFoundException
import java.time.Clock
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Service
class BarcodeReportingService(
  private val barcodeStatsService: BarcodeStatsService,
  private val javaMailSender: JavaMailSender,
  private val barcodeReportingConfig: BarcodeReportingConfig,
  private val clock: Clock,
) {

  fun distributeBarcodeStats() {
    if (barcodeReportingConfig.recipientEmails.isEmpty()) {
      throw ResourceNotFoundException("No recipients configured for the Barcode Stats Report")
    }

    val reportDate = LocalDate.now(clock).minusDays(1)
    val formattedReportDate = DateTimeFormatter.ofPattern("yyyy-MM-dd").format(reportDate)
    val emailContents = """
      Total barcodes created: ${barcodeStatsService.countBarcodesCreated()}
      Total barcodes scanned: ${barcodeStatsService.countBarcodesScanned()}
      Barcodes created on $formattedReportDate: ${barcodeStatsService.countBarcodesCreatedOnDay(reportDate)}
      Barcodes scanned on $formattedReportDate: ${barcodeStatsService.countBarcodesScannedOnDay(reportDate)}
      Unique users to have created a barcode: ${barcodeStatsService.countUniqueUsersCreatedBarcodes()}
    """.trimIndent()

    MimeMessageHelper(javaMailSender.createMimeMessage(), true)
      .apply {
        setTo(barcodeReportingConfig.recipientEmails.toTypedArray())
        setSubject("Send Legal Mail daily barcode statistics for $formattedReportDate")
        setText(emailContents)
      }.also {
        javaMailSender.send(it.mimeMessage)
      }
  }
}
