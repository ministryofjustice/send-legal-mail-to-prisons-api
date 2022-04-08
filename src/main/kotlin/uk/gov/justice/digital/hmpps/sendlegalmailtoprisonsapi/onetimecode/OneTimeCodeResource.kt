package uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.onetimecode

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Service
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.config.EmailInvalid
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.config.EmailInvalidCjsm
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.config.EmailMandatory
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.config.EmailTooLong
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.config.ErrorResponse
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.config.SessionIdMandatory
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.config.ValidationException

@RestController
@Tag(name = "one-time-code-resource")
class OneTimeCodeResource(
  private val oneTimeCodeService: OneTimeCodeService,
  private val oneTimeCodeRequestValidator: OneTimeCodeRequestValidator
) {

  @PostMapping(value = ["/oneTimeCode/email"])
  @PreAuthorize("hasRole('ROLE_SLM_EMAIL_LINK')")
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(
    summary = "Create a one time code and send to the CJSM email",
    description = "Creates a one time code and sends it in an email to the CJSM email address entered by the user.",
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "201",
        description = "One time code created and emailed",
        content = [
          Content(mediaType = "application/json")
        ],
      ),
      ApiResponse(
        responseCode = "400",
        description = "Bad request. For specific errors see the Schema for AuthenticationRequestErrorCodes",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorised, requires a valid Oauth2 token",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ]
  )
  fun createOneTimeCode(@RequestBody request: OneTimeCodeRequest) {
    oneTimeCodeRequestValidator.validate(request)
    oneTimeCodeService.createAndSendOneTimeCode(request.email, request.sessionID)
  }
}

data class OneTimeCodeRequest(
  @Schema(description = "The CJSM email address to send the one time code to", example = "andrew.barret@company.com.cjsm.net", required = true)
  val email: String,
  @Schema(description = "The browser session ID")
  val sessionID: String,
)

const val MAX_EMAIL_LENGTH = 254

@Service
class OneTimeCodeRequestValidator {
  private val emailRegex = "^[\\w!#$%&’*+/=?`{|}~^-]+(?:\\.[\\w!#$%&’*+/=?`{|}~^-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,6}$".toRegex()

  fun validate(oneTimeCodeRequest: OneTimeCodeRequest) {
    if (oneTimeCodeRequest.sessionID.isEmpty()) {
      throw ValidationException(SessionIdMandatory)
    }
    if (oneTimeCodeRequest.email.isEmpty()) {
      throw ValidationException(EmailMandatory)
    }
    if (oneTimeCodeRequest.email.length > MAX_EMAIL_LENGTH) {
      throw ValidationException(EmailTooLong)
    }
    if (emailRegex.matches(oneTimeCodeRequest.email).not()) {
      throw ValidationException(EmailInvalid)
    }
    if (oneTimeCodeRequest.email.endsWith(".cjsm.net").not()) {
      throw ValidationException(EmailInvalidCjsm)
    }
  }
}
