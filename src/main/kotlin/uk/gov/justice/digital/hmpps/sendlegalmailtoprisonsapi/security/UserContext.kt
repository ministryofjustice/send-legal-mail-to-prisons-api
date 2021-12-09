package uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.security

import org.springframework.stereotype.Component

@Component
class UserContext {
  private val _authToken = ThreadLocal<String>()
  private val _caseload = ThreadLocal<String>()
  var authToken: String
    get() = _authToken.get()
    set(value) = _authToken.set(value)
  var caseload: String
    get() = _caseload.get()
    set(value) = _caseload.set(value)
}
