package uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.prisons

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.config.ErrorResponse

@RestController
@RequestMapping(produces = [MediaType.APPLICATION_JSON_VALUE])
class SupportedPrisonsResource(private val supportedPrisonsService: SupportedPrisonsService) {

  @GetMapping(value = ["/prisons"])
  @ResponseBody
  @ResponseStatus(HttpStatus.OK)
  @PreAuthorize("hasRole('ROLE_SLM_ADMIN')")
  @Operation(
    summary = "Retrieve a list of supported prisons",
    security = [SecurityRequirement(name = "ROLE_SLM_ADMIN")]
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Supported prisons returned",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = SupportedPrisons::class))],
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorised, requires a valid CJSM email link token",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Forbidden, requires a valid token with role ROLE_SLM_CREATE_BARCODE",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ]
  )
  fun getSupportedPrisons(): SupportedPrisons = SupportedPrisons(supportedPrisonsService.findSupportedPrisonCodes())
}

data class SupportedPrisons(
  @Schema(description = "The prison codes of the supported prisons")
  val supportedPrisons: List<String>
)
