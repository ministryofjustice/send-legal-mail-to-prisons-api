package uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.magiclink

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
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
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.config.ValidationException

@RestController
@Tag(name = "email-link-resource")
class MagicLinkResource(
  private val magicLinkService: MagicLinkService,
  private val magicLinkRequestValidator: MagicLinkRequestValidator,
) {

  @PostMapping(value = ["/link/email"])
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(
    summary = "Create and send a CJSM email link",
    description = "Creates a CJSM email link and send to the email address entered by the user.",
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "201",
        description = "CJSM email link created",
        content = [
          Content(mediaType = "application/json")
        ],
      ),
      ApiResponse(
        responseCode = "400",
        description = "Bad request. For specific errors see the Schema for MagicLinkRequestErrorCodes",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorised, requires a valid Oauth2 token",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ]
  )
  fun createMagicLink(@RequestBody request: MagicLinkRequest) {
    magicLinkRequestValidator.validate(request)
    magicLinkService.createAndSendMagicLink(request.email)
  }

  @PostMapping(value = ["/link/verify"])
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(
    summary = "Verify a CJSM email link secret",
    description = "Verifies a CJSM email link secret and swaps it for an authentication token if valid.",
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "201",
        description = "Authentication token created",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = VerifyLinkResponse::class))],
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorised, requires a valid Oauth2 token",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "404",
        description = "Not found, unable to verify the CJSM email link",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ]
  )
  fun verifyMagicLink(@RequestBody request: VerifyLinkRequest) =
    VerifyLinkResponse(magicLinkService.verifyMagicLinkSecret(request.secret))
}

data class MagicLinkRequest(
  @Schema(description = "The email address to send the CJSM email link to", example = "andrew.barret@company.com", required = true)
  val email: String,
)

data class VerifyLinkRequest(
  @Schema(description = "The secret to verify", required = true)
  val secret: String,
)

data class VerifyLinkResponse(
  @Schema(description = "The JWT")
  val token: String,
)

const val MAX_EMAIL_LENGTH = 254

@Service
class MagicLinkRequestValidator {
  private val emailRegex = "^[\\w!#$%&’*+/=?`{|}~^-]+(?:\\.[\\w!#$%&’*+/=?`{|}~^-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,6}$".toRegex()

  fun validate(magicLinkRequest: MagicLinkRequest) {
    if (magicLinkRequest.email.isEmpty()) {
      throw ValidationException(EmailMandatory)
    }
    if (magicLinkRequest.email.length > MAX_EMAIL_LENGTH) {
      throw ValidationException(EmailTooLong)
    }
    if (emailRegex.matches(magicLinkRequest.email).not()) {
      throw ValidationException(EmailInvalid)
    }
    if (magicLinkRequest.email.endsWith(".cjsm.net").not()) {
      throw ValidationException(EmailInvalidCjsm)
    }
  }
}
