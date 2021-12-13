package uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.barcode

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Duration

class RandomCheckServiceTest {

  @Test
  fun `should never require random check`() {
    val randomCheckService = RandomCheckService(BarcodeConfig(Duration.ZERO, 0))

    repeat(10) {
      assertThat(randomCheckService.requiresRandomCheck()).isFalse
    }
  }

  @Test
  fun `should always require random check`() {
    val randomCheckService = RandomCheckService(BarcodeConfig(Duration.ZERO, 100))

    repeat(10) {
      assertThat(randomCheckService.requiresRandomCheck()).isTrue
    }
  }

  @Test
  fun `should sometimes require random check`() {
    val randomCheckService = RandomCheckService(BarcodeConfig(Duration.ZERO, 50))

    var randomChecksCount = 0
    var noChecksCount = 0
    repeat(20) {
      if (randomCheckService.requiresRandomCheck()) {
        randomChecksCount++
      } else {
        noChecksCount++
      }
    }

    assertThat(randomChecksCount).isGreaterThan(0)
    assertThat(noChecksCount).isGreaterThan(0)
  }
}
