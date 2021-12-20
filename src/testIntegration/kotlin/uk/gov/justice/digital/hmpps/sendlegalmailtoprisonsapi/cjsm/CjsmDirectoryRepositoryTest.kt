package uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.cjsm

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.IntegrationTest

class CjsmDirectoryRepositoryTest : IntegrationTest() {

  @Test
  fun `find by unique column secure email returns a single result`() {
    cjsmDirectoryRepository.save(CjsmDirectoryEntry(secureEmail = "someone@company.com.cjsm.net", firstName = "Someone", lastName = "Someoneson", organisation = "Company", townCity = "Sometown", businessType = "Some business type"))

    val directoryEntry = cjsmDirectoryRepository.findBySecureEmail("someone@company.com.cjsm.net")
    assertThat(directoryEntry?.firstName).isEqualTo("Someone")
  }

  @Test
  fun `find by unique column secure email returns a null if not found`() {
    val noDirectoryEntry = cjsmDirectoryRepository.findBySecureEmail("neverheardofhim@company.com.cjsm.net")
    assertThat(noDirectoryEntry).isNull()
  }
}
