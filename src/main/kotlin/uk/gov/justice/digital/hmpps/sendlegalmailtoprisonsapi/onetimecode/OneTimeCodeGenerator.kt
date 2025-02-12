package uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.onetimecode

import org.springframework.stereotype.Component

@Component
class OneTimeCodeGenerator(val oneTimeCodeConfig: OneTimeCodeConfig) {

  fun generateOneTimeCode(): String = 1.rangeTo(oneTimeCodeConfig.codeLength).map {
    oneTimeCodeConfig.supportedCharacters[oneTimeCodeConfig.supportedCharacters.indices.random()]
  }.joinToString("")
}
