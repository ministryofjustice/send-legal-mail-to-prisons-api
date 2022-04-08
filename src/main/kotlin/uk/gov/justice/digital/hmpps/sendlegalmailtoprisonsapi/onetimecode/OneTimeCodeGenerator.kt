package uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.onetimecode

import org.springframework.stereotype.Component

@Component
class OneTimeCodeGenerator {

  private companion object {
    const val CODE_LENGTH = 4
    val SUPPORTED_CHARACTERS = setOf(
      'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M',
      'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z',
    ).toList()
  }

  fun generateOneTimeCode(): String =
    1.rangeTo(CODE_LENGTH).map { SUPPORTED_CHARACTERS[SUPPORTED_CHARACTERS.indices.random()] }.joinToString("")
}
