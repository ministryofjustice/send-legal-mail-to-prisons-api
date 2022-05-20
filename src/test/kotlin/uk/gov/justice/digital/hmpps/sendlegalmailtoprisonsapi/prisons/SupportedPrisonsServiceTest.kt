package uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.prisons

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.given
import org.mockito.kotlin.mock
import org.mockito.kotlin.then
import java.time.Instant

class SupportedPrisonsServiceTest {

  private val supportedPrisonsRepository = mock<SupportedPrisonsRepository>()
  private val supportedPrisonsService = SupportedPrisonsService(supportedPrisonsRepository)

  @Test
  fun `should return empty list if none`() {
    given { supportedPrisonsRepository.findByActive(any()) }.willReturn(listOf())

    val supportedPrisons = supportedPrisonsService.findSupportedPrisonCodes()

    then(supportedPrisonsRepository).should().findByActive(true)
    assertThat(supportedPrisons).isEmpty()
  }

  @Test
  fun `should return list of prison codes`() {
    given { supportedPrisonsRepository.findByActive(any()) }
      .willReturn(listOf(aSupportedPrison("AAA"), aSupportedPrison("BBB")))

    val supportedPrisons = supportedPrisonsService.findSupportedPrisonCodes()

    assertThat(supportedPrisons).containsExactly("AAA", "BBB")
  }

  @Test
  fun `should return prison codes in alphabetical order`() {
    given { supportedPrisonsRepository.findByActive(any()) }
      .willReturn(listOf(aSupportedPrison("BBB"), aSupportedPrison("CCC"), aSupportedPrison("AAA")))

    val supportedPrisons = supportedPrisonsService.findSupportedPrisonCodes()

    assertThat(supportedPrisons).containsExactly("AAA", "BBB", "CCC")
  }

  private fun aSupportedPrison(code: String): SupportedPrison =
    SupportedPrison(code = code, active = true, updatedBy = "anyone", updated = Instant.now())
}
