package uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.barcode

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.jdbc.Sql
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.IntegrationTest
import java.time.Instant

@Sql("/barcode/barcodeStatsServiceTest.sql")
class BarcodeStatsServiceTest : IntegrationTest() {

  @Autowired
  private lateinit var barcodeStatsService: BarcodeStatsService

  @Test
  fun `should count number of barcodes created`() {
    val count = barcodeStatsService.countBarcodesCreated()

    assertThat(count).isEqualTo(7L)
  }

  @Test
  fun `should count number of barcodes created on a particular day`() {
    val dayOfTest = Instant.ofEpochMilli(1646222400000) // 2022-03-02 12:00:00

    val count = barcodeStatsService.countBarcodesCreatedOnDay(dayOfTest)

    assertThat(count).isEqualTo(4L)
  }

  @Test
  fun `should count number of barcodes scanned`() {
    val count = barcodeStatsService.countBarcodesScanned()

    assertThat(count).isEqualTo(4L)
  }

  @Test
  fun `should count number of barcodes scanned on a particular day`() {
    val dayOfTest = Instant.ofEpochMilli(1646222400000) // 2022-03-02 12:00:00

    val count = barcodeStatsService.countBarcodesScannedOnDay(dayOfTest)

    assertThat(count).isEqualTo(2L)
  }

  @Test
  fun `should count unique users who have created a barcode`() {
    val count = barcodeStatsService.countUniqueUsersCreatedBarcodes()

    assertThat(count).isEqualTo(3L)
  }
}
