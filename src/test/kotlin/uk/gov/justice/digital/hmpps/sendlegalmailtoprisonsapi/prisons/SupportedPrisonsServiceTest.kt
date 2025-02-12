package uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.prisons

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyString
import org.mockito.kotlin.any
import org.mockito.kotlin.check
import org.mockito.kotlin.given
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.then
import org.mockito.kotlin.willReturn
import uk.gov.justice.digital.hmpps.prisonregister.model.PrisonDto
import uk.gov.justice.digital.hmpps.prisonregister.model.PrisonTypeDto
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.client.PrisonRegisterClient
import java.time.Instant
import java.util.Optional

class SupportedPrisonsServiceTest {

  private val supportedPrisonsRepository = mock<SupportedPrisonsRepository>()
  private val prisonRegisterClient = mock<PrisonRegisterClient>()
  private val supportedPrisonsService = SupportedPrisonsService(supportedPrisonsRepository, prisonRegisterClient)

  @Nested
  inner class FindSupportedPrisonCodes {
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
  }

  @Nested
  inner class AddPrison {
    @Test
    fun `should return null if prison code not found`() {
      given { prisonRegisterClient.getPrison(anyString()) }.willReturn(null)

      val newSupportedPrisonCode = supportedPrisonsService.addPrisonCode("some-prison")

      then(prisonRegisterClient).should().getPrison("some-prison")
      then(supportedPrisonsRepository).should(never()).save(any())
      assertThat(newSupportedPrisonCode).isNull()
    }

    @Test
    fun `should return null if prison not active`() {
      given { prisonRegisterClient.getPrison(anyString()) }.willReturn(aPrisonDto("some-prison", active = false))

      val newSupportedPrisonCode = supportedPrisonsService.addPrisonCode("some-prison")

      then(prisonRegisterClient).should().getPrison("some-prison")
      then(supportedPrisonsRepository).should(never()).save(any())
      assertThat(newSupportedPrisonCode).isNull()
    }

    @Test
    fun `should save prison code if found`() {
      given { prisonRegisterClient.getPrison(anyString()) }.willReturn(aPrisonDto("some-prison"))
      given { supportedPrisonsRepository.save(any()) }.willReturn(aSupportedPrison("some-prison"))

      val newSupportedPrisonCode = supportedPrisonsService.addPrisonCode("some-prison")

      then(supportedPrisonsRepository).should().save(
        check {
          assertThat(it.code).isEqualTo("some-prison")
          assertThat(it.active).isTrue
        },
      )
      assertThat(newSupportedPrisonCode).isEqualTo("some-prison")
    }
  }

  @Nested
  inner class RemovePrisonCode {
    @Test
    fun `should return null if prison code not found`() {
      given { supportedPrisonsRepository.findById(anyString()) }.willReturn { Optional.empty() }

      val removedPrisonCode = supportedPrisonsService.removePrisonCode("some-prison")

      assertThat(removedPrisonCode).isNull()
      then(supportedPrisonsRepository).should().findById("some-prison")
      then(supportedPrisonsRepository).should(never()).save(any())
    }

    @Test
    fun `should remove prison code if active`() {
      given { supportedPrisonsRepository.findById(anyString()) }
        .willReturn { Optional.of(aSupportedPrison(code = "some-prison")) }

      val removedPrisonCode = supportedPrisonsService.removePrisonCode("some-prison")

      assertThat(removedPrisonCode).isEqualTo("some-prison")
      then(supportedPrisonsRepository).should().save(
        check {
          assertThat(it.code).isEqualTo("some-prison")
          assertThat(it.active).isFalse()
        },
      )
    }

    @Test
    fun `should remove prison code if already inactive`() {
      given { supportedPrisonsRepository.findById(anyString()) }
        .willReturn { Optional.of(aSupportedPrison(code = "some-prison", active = false)) }

      val removedPrisonCode = supportedPrisonsService.removePrisonCode("some-prison")

      assertThat(removedPrisonCode).isEqualTo("some-prison")
      then(supportedPrisonsRepository).should().save(
        check {
          assertThat(it.code).isEqualTo("some-prison")
          assertThat(it.active).isFalse()
        },
      )
    }
  }

  private fun aSupportedPrison(code: String = "some-prison", active: Boolean = true): SupportedPrison = SupportedPrison(code = code, active = active, updatedBy = "anyone", updated = Instant.now())

  private fun aPrisonDto(code: String, active: Boolean = true): PrisonDto = PrisonDto(
    prisonId = code,
    prisonName = "Any name",
    active = active,
    male = true,
    female = true,
    contracted = false,
    types = listOf(PrisonTypeDto(PrisonTypeDto.Code.hMP, "hmp")),
    addresses = listOf(),
  )
}
