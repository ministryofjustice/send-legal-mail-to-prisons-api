package uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.cjsm

import io.swagger.v3.oas.annotations.Hidden
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.config.ErrorResponse
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.config.ResourceNotFoundException
import org.springframework.security.core.userdetails.UserDetails as PrincipalUserDetails

@RestController
@RequestMapping(produces = [MediaType.APPLICATION_JSON_VALUE])
class CjsmResource(private val cjsmService: CjsmService) {

  @Hidden
  @PostMapping(value = ["/cjsm/directory/refresh"])
  fun refreshCjsmDirectory() = cjsmService.saveCjsmDirectoryCsv()

  @GetMapping(value = ["/cjsm/user/me"])
  @ResponseBody
  @PreAuthorize("hasRole('ROLE_SLM_CREATE_BARCODE')")
  @Operation(
    summary = "Retrieve information about the curren user",
    security = [SecurityRequirement(name = "ROLE_SLM_CREATE_BARCODE")],
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "OK",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = UserDetails::class))],
      ),
      ApiResponse(
        responseCode = "400",
        description = "Bad request",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorised, requires a valid magic link token",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "404",
        description = "Not found, the email is not in the CJSM directory",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  fun getUserDetails(
    @Parameter(hidden = true) @AuthenticationPrincipal principalUserDetails: PrincipalUserDetails,
  ): UserDetails =
    cjsmService.findUser(principalUserDetails.username)
      ?: throw ResourceNotFoundException("User ${principalUserDetails.username} not in CJSM directory")
}

data class UserDetails(
  @Schema(description = "The ID of the user", example = "some.user@some.solicitors.cjsm.net")
  val userId: String,

  @Schema(description = "The organisation of the user", example = "Some Solicitors Ltd")
  val organisation: String? = null,

  @Schema(description = "The organisation type", example = "Barristers")
  val organisationType: String? = null,

  @Schema(description = "The user's town or city", example = "London")
  val townOrCity: String? = null,
)
