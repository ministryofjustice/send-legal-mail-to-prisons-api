package uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.magiclink

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.config.ErrorResponse
import javax.servlet.http.HttpServletRequest
import javax.validation.constraints.NotEmpty
import javax.validation.constraints.NotNull

@RestController
class MagicLinkResource(private val magicLinkService: MagicLinkService) {

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
  fun createMagicLink(@RequestBody @NotEmpty request: MagicLinkRequest, httpReq: HttpServletRequest) {
    magicLinkService.createAndSendMagicLink(request.email)
  }

  data class MagicLinkRequest(
    @Schema(description = "The email address to send the magic link to", example = "andrew.barret@company.com")
    @NotNull
    val email: String,
  )
}
