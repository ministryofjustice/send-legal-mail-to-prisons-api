package uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.security

import org.springframework.stereotype.Component

@Component
object UserContext {
  private val authToken = ThreadLocal<String>()
  private val caseload = ThreadLocal<String>()
  fun setAuthToken(token: String?) = authToken.set(token)
  fun getAuthToken(): String = authToken.get()
  fun setCaseload(caseload: String) = this.caseload.set(caseload)
  fun getCaseload(): String = this.caseload.get()
}
