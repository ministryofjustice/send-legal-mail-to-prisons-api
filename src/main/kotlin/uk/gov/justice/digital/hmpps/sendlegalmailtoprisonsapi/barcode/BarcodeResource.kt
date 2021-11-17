package uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.barcode

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import org.springframework.http.HttpStatus.CREATED
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.config.ErrorResponse
import javax.validation.constraints.NotEmpty
import javax.validation.constraints.NotNull

@RestController
@RequestMapping(produces = [MediaType.APPLICATION_JSON_VALUE])
class BarcodeResource(private val barcodeService: BarcodeService) {

  @PostMapping(value = ["/barcode"])
  @ResponseBody
  @ResponseStatus(CREATED)
  // TODO this will be protected by a role
  // @PreAuthorize("hasRole('ROLE_CREATE_BARCODE')")
  @Operation(
    summary = "Creates a one time barcode for the prisoner",
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "201",
        description = "Barcode created",
        content = [
          Content(mediaType = "application/json")
        ],
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
        description = "Not found",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      )
    ]
  )
  fun createBarcode(@RequestBody @NotEmpty request: CreateBarcodeRequest): String =
    barcodeService.createBarcode(request.userId)
}

data class CreateBarcodeRequest(
  @Schema(description = "The user creating a barcode, probably an email address", example = "andrew.barret@company.com")
  @NotNull
  val userId: String
)
