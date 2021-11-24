package uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.magiclink

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.config.ErrorCode
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.config.ErrorResponse
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.config.ValidationException
import javax.servlet.http.HttpServletRequest

@RestController
class MagicLinkResource(
  private val magicLinkService: MagicLinkService,
  private val magicLinkRequestValidator: MagicLinkRequestValidator,
) {

  @PostMapping(value = ["/link/email"])
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(
    summary = "Create and send a magic link",
    description = "Creates a magic link and send to the email address entered by the user.",
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "201",
        description = "Magic link created",
        content = [
          Content(mediaType = "application/json")
        ],
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

  data class MagicLinkRequest(
    @Schema(description = "The email address to send the magic link to", example = "andrew.barret@company.com", required = true)
    val email: String,
  )

  @PostMapping(value = ["/link/verify"])
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(
    summary = "Verify a magic link secret",
    description = "Verifies a magic link secret and swaps it for an authentication token if valid.",
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "201",
        description = "Authentication token created",
        content = [
          Content(mediaType = "application/json")
        ],
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorised, requires a valid Oauth2 token",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "404",
        description = "Not found, unable to verify the magic link",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ]
  )
  fun verifyMagicLink(@RequestBody request: VerifyLinkRequest, httpReq: HttpServletRequest) =
    magicLinkService.verifyMagicLinkSecret(request.secret)

  data class VerifyLinkRequest(
    @Schema(description = "The secret to verify", required = true)
    val secret: String,
  )
}

@Service
class MagicLinkRequestValidator {
  private val emailRegex = "^[\\w!#$%&’*+/=?`{|}~^-]+(?:\\.[\\w!#$%&’*+/=?`{|}~^-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,6}$".toRegex()

  fun validate(magicLinkRequest: MagicLinkResource.MagicLinkRequest) {
    if (magicLinkRequest.email.isEmpty()) {
      throw ValidationException(ErrorCode.EMAIL_MANDATORY)
    }
    if (emailRegex.matches(magicLinkRequest.email).not()) {
      throw ValidationException(ErrorCode.INVALID_EMAIL)
    }
    if (magicLinkRequest.email.endsWith(".cjsm.net").not()) {
      throw ValidationException(ErrorCode.INVALID_CJSM_EMAIL)
    }
  }
}
