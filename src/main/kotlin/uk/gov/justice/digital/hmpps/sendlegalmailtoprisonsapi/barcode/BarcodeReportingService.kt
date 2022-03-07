package uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.barcode

import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.MimeMessageHelper
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Service
class BarcodeReportingService(
  private val barcodeStatsService: BarcodeStatsService,
  private val javaMailSender: JavaMailSender,
  private val barcodeReportingConfig: BarcodeReportingConfig,
) {

  fun distributeBarcodeStats() {
    if (barcodeReportingConfig.recipientEmails.isEmpty()) {
      return
    }

    val reportDate = LocalDate.now().minusDays(1)
    val formattedReportDate = DateTimeFormatter.ofPattern("YYYY-MM-DD").format(reportDate)
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
