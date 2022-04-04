package uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.prisonersearch

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.prisonersearch.model.Prisoner
import uk.gov.justice.digital.hmpps.prisonersearch.model.PrisonerAlias
import java.time.LocalDate

class PrisonerMatchCountTest {

  @Nested
  inner class CreateFromResultsOfAPrisonNumberSearch {
    val prisonerSearchRequest = PrisonerSearchRequest("A1234BC", "John", "Smith", null)

    @Test
    fun `prison number and main name match`() {
      val prisoners = listOf(
        aPrisoner.copy(prisonerNumber = "A1234BC", firstName = "JOHN", lastName = "SMITH")
      )

      val prisonerMatch = PrisonerMatch.of(prisoners, prisonerSearchRequest)

      assertThat(prisonerMatch).isEqualTo(PrisonerMatch(mainDetailsCount = 1, aliasDetailsCount = 0))
    }

    @Test
    fun `prison number and alias name match`() {
      val prisoners = listOf(
        aPrisoner.copy(
          prisonerNumber = "A1234BC", firstName = "JOHNNY", lastName = "SMITH",
          aliases = listOf(
            PrisonerAlias("John", lastName = "Doe", dateOfBirth = LocalDate.MIN),
            PrisonerAlias("John", lastName = "SMITH", dateOfBirth = LocalDate.MIN),
            PrisonerAlias("John", lastName = "SMITH", dateOfBirth = LocalDate.MAX),
            PrisonerAlias("Frank", lastName = "SMITH", dateOfBirth = LocalDate.MIN)
          )
        )
      )

      val prisonerMatch = PrisonerMatch.of(prisoners, prisonerSearchRequest)

      assertThat(prisonerMatch).isEqualTo(PrisonerMatch(mainDetailsCount = 0, aliasDetailsCount = 1))
    }

    @Test
    fun `prison number and main name match, ignore any alias match as they are the same person`() {
      val prisoners = listOf(
        aPrisoner.copy(
          prisonerNumber = "A1234BC", firstName = "JOHN", lastName = "SMITH",
          aliases = listOf(
            PrisonerAlias("John", lastName = "Doe", dateOfBirth = LocalDate.MIN),
            PrisonerAlias("John", lastName = "SMITH", dateOfBirth = LocalDate.MIN),
            PrisonerAlias("John", lastName = "SMITH", dateOfBirth = LocalDate.MAX),
            PrisonerAlias("Frank", lastName = "SMITH", dateOfBirth = LocalDate.MIN)
          )
        )
      )

      val prisonerMatch = PrisonerMatch.of(prisoners, prisonerSearchRequest)

      assertThat(prisonerMatch).isEqualTo(PrisonerMatch(mainDetailsCount = 1, aliasDetailsCount = 0))
    }

    @Test
    fun `prison number matches but names dont`() {
      val prisoners = listOf(
        aPrisoner.copy(
          prisonerNumber = "A1234BC", firstName = "JOHNNY", lastName = "SMITH",
          aliases = listOf(
            PrisonerAlias("John", lastName = "Doe", dateOfBirth = LocalDate.MIN),
            PrisonerAlias("Frank", lastName = "SMITH", dateOfBirth = LocalDate.MIN)
          )
        )
      )

      val prisonerMatch = PrisonerMatch.of(prisoners, prisonerSearchRequest)

      assertThat(prisonerMatch).isEqualTo(PrisonerMatch(mainDetailsCount = 0, aliasDetailsCount = 0))
    }

    @Test
    fun `prison number doesnt match but name does`() {
      val prisoners = listOf(
        aPrisoner.copy(prisonerNumber = "Z1234AA", firstName = "JOHN", lastName = "SMITH")
      )

      val prisonerMatch = PrisonerMatch.of(prisoners, prisonerSearchRequest)

      assertThat(prisonerMatch).isEqualTo(PrisonerMatch(mainDetailsCount = 0, aliasDetailsCount = 0))
    }
  }

  @Nested
  inner class CreateFromResultsOfANameAndDobSearch {
    val dob = LocalDate.of(1980, 2, 17)
    val prisonerSearchRequest = PrisonerSearchRequest(null, "John", "Smith", dob)

    @Test
    fun `main name and dob match`() {
      val prisoners = listOf(
        aPrisoner.copy(prisonerNumber = "A1234BC", firstName = "JOHN", lastName = "SMITH", dateOfBirth = dob)
      )

      val prisonerMatch = PrisonerMatch.of(prisoners, prisonerSearchRequest)

      assertThat(prisonerMatch).isEqualTo(PrisonerMatch(mainDetailsCount = 1, aliasDetailsCount = 0))
    }

    @Test
    fun `main name matches several but dob match only matches one`() {
      val prisoners = listOf(
        aPrisoner.copy(prisonerNumber = "A1234BC", firstName = "JOHN", lastName = "SMITH", dateOfBirth = LocalDate.MIN),
        aPrisoner.copy(prisonerNumber = "B1234BC", firstName = "JOHN", lastName = "SMITH", dateOfBirth = dob),
        aPrisoner.copy(prisonerNumber = "C1234BC", firstName = "JOHN", lastName = "SMITH", dateOfBirth = LocalDate.MAX),
      )

      val prisonerMatch = PrisonerMatch.of(prisoners, prisonerSearchRequest)

      assertThat(prisonerMatch).isEqualTo(PrisonerMatch(mainDetailsCount = 1, aliasDetailsCount = 0))
    }

    @Test
    fun `dob and alias name match`() {
      val prisoners = listOf(
        aPrisoner.copy(
          prisonerNumber = "A1234BC", firstName = "JOHNNY", lastName = "SMITH", dateOfBirth = dob,
          aliases = listOf(
            PrisonerAlias("John", lastName = "Doe", dateOfBirth = LocalDate.MIN),
            PrisonerAlias("John", lastName = "SMITH", dateOfBirth = dob),
            PrisonerAlias("John", lastName = "SMITH", dateOfBirth = LocalDate.MAX),
            PrisonerAlias("Frank", lastName = "SMITH", dateOfBirth = LocalDate.MIN)
          )
        )
      )

      val prisonerMatch = PrisonerMatch.of(prisoners, prisonerSearchRequest)

      assertThat(prisonerMatch).isEqualTo(PrisonerMatch(mainDetailsCount = 0, aliasDetailsCount = 1))
    }

    @Test
    fun `dob and several aliases name match - only count the first alias match`() {
      val prisoners = listOf(
        aPrisoner.copy(
          prisonerNumber = "A1234BC", firstName = "JOHNNY", lastName = "SMITH", dateOfBirth = dob,
          aliases = listOf(
            PrisonerAlias("John", lastName = "SMITH", dateOfBirth = dob),
            PrisonerAlias("John", lastName = "SMITH", dateOfBirth = dob)
          )
        )
      )

      val prisonerMatch = PrisonerMatch.of(prisoners, prisonerSearchRequest)

      assertThat(prisonerMatch).isEqualTo(PrisonerMatch(mainDetailsCount = 0, aliasDetailsCount = 1))
    }

    @Test
    fun `a mix of name and dob matches on different prisoners`() {
      val prisoners = listOf(
        aPrisoner.copy(
          prisonerNumber = "Z1234BC",
          firstName = "JOHN",
          lastName = "SMITH",
          dateOfBirth = dob
        ), // match main details
        aPrisoner.copy(
          prisonerNumber = "A1234BC", firstName = "JOHNNY", lastName = "SMITH", dateOfBirth = dob,
          aliases = listOf(
            PrisonerAlias("John", lastName = "SMITH", dateOfBirth = dob), // match alias details
            PrisonerAlias("John", lastName = "SMITH", dateOfBirth = dob)
          )
        ),
        aPrisoner.copy(
          prisonerNumber = "Z9999BC",
          firstName = "JOHN",
          lastName = "SMITH",
          dateOfBirth = LocalDate.MIN
        ), // no match
        aPrisoner.copy(
          prisonerNumber = "Z9999BC",
          firstName = "JOHN",
          lastName = "SMITHSON",
          dateOfBirth = LocalDate.MIN,
          aliases = listOf(
            PrisonerAlias("John", lastName = "SMITH", dateOfBirth = dob) // match alias details
          )
        )
      )

      val prisonerMatch = PrisonerMatch.of(prisoners, prisonerSearchRequest)

      assertThat(prisonerMatch).isEqualTo(PrisonerMatch(mainDetailsCount = 1, aliasDetailsCount = 2))
    }

    @Test
    fun `dob matches but names dont`() {
      val prisoners = listOf(
        aPrisoner.copy(
          prisonerNumber = "A1234BC", firstName = "JOHNNY", lastName = "SMITH", dateOfBirth = dob,
          aliases = listOf(
            PrisonerAlias("John", lastName = "Doe", dateOfBirth = dob),
            PrisonerAlias("Frank", lastName = "SMITH", dateOfBirth = dob)
          )
        )
      )

      val prisonerMatch = PrisonerMatch.of(prisoners, prisonerSearchRequest)

      assertThat(prisonerMatch).isEqualTo(PrisonerMatch(mainDetailsCount = 0, aliasDetailsCount = 0))
    }

    @Test
    fun `dob doesnt match but name does`() {
      val prisoners = listOf(
        aPrisoner.copy(prisonerNumber = "Z1234AA", firstName = "JOHN", lastName = "SMITH", dateOfBirth = LocalDate.MIN)
      )

      val prisonerMatch = PrisonerMatch.of(prisoners, prisonerSearchRequest)

      assertThat(prisonerMatch).isEqualTo(PrisonerMatch(mainDetailsCount = 0, aliasDetailsCount = 0))
    }
  }

  private val aPrisoner = Prisoner(restrictedPatient = false)
}
