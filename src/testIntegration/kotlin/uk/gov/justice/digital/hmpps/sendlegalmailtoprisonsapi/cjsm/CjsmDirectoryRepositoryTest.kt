package uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.cjsm

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.IntegrationTest

class CjsmDirectoryRepositoryTest : IntegrationTest() {

  @Test
  fun `can create and retrieve a CJSM directory entry`() {
    val cjsmDirectoryEntry = CjsmDirectoryEntry(
      secureEmail = "some.user@company.com.cjsm.net",
      firstName = "Some",
      lastName = "User",
      organisation = "Aardvark Solicitors",
      townCity = "Sheffield",
      businessType = "Barristers"
    )

    val savedCjsmDirectoryEntry = cjsmDirectoryRepository.save(cjsmDirectoryEntry)

    assertThat(savedCjsmDirectoryEntry)
      .extracting("secureEmail", "firstName", "lastName", "organisation", "townCity", "businessType")
      .isEqualTo(listOf("some.user@company.com.cjsm.net", "Some", "User", "Aardvark Solicitors", "Sheffield", "Barristers"))
  }
}
