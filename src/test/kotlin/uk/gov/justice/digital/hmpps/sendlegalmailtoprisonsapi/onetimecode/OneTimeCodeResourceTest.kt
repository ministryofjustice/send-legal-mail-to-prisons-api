package uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.onetimecode

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.given
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.config.SessionIdMandatory
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.config.ValidationException

class OneTimeCodeResourceTest {

  private val oneTimeCodeService = mock<OneTimeCodeService>()
  private val oneTimeCodeRequestValidator = mock<OneTimeCodeRequestValidator>()
  private val oneTimeCodeResource = OneTimeCodeResource(oneTimeCodeService, oneTimeCodeRequestValidator)

  @Nested
  inner class CreateOneTimeCode {
    @Test
    fun `should request code via service given request validation passes`() {
      val oneTimeCodeRequest = OneTimeCodeRequest("someone@soliciters.cjsm.net", "12345678")

      oneTimeCodeResource.createOneTimeCode(oneTimeCodeRequest)

      verify(oneTimeCodeRequestValidator).validate(oneTimeCodeRequest)
      verify(oneTimeCodeService).createAndSendOneTimeCode("someone@soliciters.cjsm.net", "12345678")
    }

    @Test
    fun `should not request code via service given request validation fails`() {
      val oneTimeCodeRequest = OneTimeCodeRequest("someone@soliciters.cjsm.net", "")
      given { oneTimeCodeRequestValidator.validate(any()) }.willThrow(ValidationException(SessionIdMandatory))

      assertThatThrownBy {
        oneTimeCodeResource.createOneTimeCode(oneTimeCodeRequest)
      }.isInstanceOf(ValidationException::class.java)
        .extracting("errorCode").isEqualTo(SessionIdMandatory)

      verify(oneTimeCodeRequestValidator).validate(oneTimeCodeRequest)
      verifyNoInteractions(oneTimeCodeService)
    }
  }

  @Nested
  inner class VerifyOneTimeCode {
    @Test
    fun `should verify one time code`() {
      val verifyCodeRequest = VerifyCodeRequest("ABCD", "12345678")
      given { oneTimeCodeService.verifyOneTimeCode(any(), any()) }.willReturn("a-valid-jwt")

      val verifyCodeResponse = oneTimeCodeResource.verifyMagicLink(verifyCodeRequest)

      assertThat(verifyCodeResponse.token).isEqualTo("a-valid-jwt")
      verify(oneTimeCodeService).verifyOneTimeCode("ABCD", "12345678")
    }
  }
}
