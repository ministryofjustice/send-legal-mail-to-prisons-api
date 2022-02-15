package uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.barcode

import com.fasterxml.jackson.annotation.JsonFormat
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import org.springframework.http.HttpStatus.CREATED
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.config.ErrorResponse
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.security.UserContext
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.validators.validateRequestHasDobOrPrisonNumber
import java.time.LocalDate
import javax.validation.Valid
import javax.validation.constraints.Pattern
import javax.validation.constraints.Size

@RestController
@RequestMapping(produces = [MediaType.APPLICATION_JSON_VALUE])
class BarcodeResource(private val barcodeService: BarcodeService, private val userContext: UserContext) {

  @PostMapping(value = ["/barcode"])
  @ResponseBody
  @ResponseStatus(CREATED)
  @PreAuthorize("hasRole('ROLE_SLM_CREATE_BARCODE')")
  @Operation(
    summary = "Creates a one time barcode for the prisoner",
    security = [SecurityRequirement(name = "ROLE_SLM_CREATE_BARCODE")]
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "201",
        description = "Barcode created",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = CreateBarcodeResponse::class))],
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
    ]
  )
  fun createBarcode(
    @Parameter(hidden = true) @AuthenticationPrincipal userDetails: UserDetails,
    @RequestBody @Valid createBarcodeRequest: CreateBarcodeRequest
  ): CreateBarcodeResponse {
    validateRequestHasDobOrPrisonNumber(createBarcodeRequest)
    return CreateBarcodeResponse(barcodeService.createBarcode(userDetails.username, createBarcodeRequest))
  }

  @PostMapping(value = ["/barcode/check"])
  @ResponseBody
  @PreAuthorize("hasRole('ROLE_SLM_SCAN_BARCODE')")
  @Operation(
    summary = "Checks the status of a barcode received on Rule 39 mail",
    security = [SecurityRequirement(name = "ROLE_SLM_SCAN_BARCODE")]
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Barcode is OK and no further checks are required",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = CheckBarcodeResponse::class))],
      ),
      ApiResponse(
        responseCode = "400",
        description = "Bad request. For specific errors see the Schema for CheckBarcodeErrorCodes",
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
  fun checkBarcode(
    @Parameter(hidden = true) @AuthenticationPrincipal userId: String,
    @RequestBody request: CheckBarcodeRequest,
  ) = CheckBarcodeResponse(barcodeService.checkBarcode(userId, request.barcode, userContext.caseload))
}

data class CreateBarcodeRequest(
  @Schema(description = "The recipient name", example = "John Doe", required = true)
  @field:Pattern(regexp = "^[a-zA-Z '`-]+$")
  @field:Size(max = 60)
  val prisonerName: String,

  @Schema(description = "The ID of the prison where the recipient is located", example = "BXI", required = true)
  @field:Pattern(regexp = "^[A-Z]{3}$")
  val prisonId: String,

  @Schema(description = "The date of birth of the recipient if known", example = "1965-04-23", required = false)
  @field:JsonFormat(pattern = "yyyy-MM-dd")
  val dob: LocalDate? = null,

  @Schema(description = "The prison number of the recipient if known", example = "A1234BC", required = false)
  @field:Pattern(regexp = "^[A-Z]\\d{4}[A-Z]{2}$")
  val prisonNumber: String? = null,

  @Schema(description = "The ID of the contact if known", example = "1234", required = false)
  val contactId: Long? = null,
)

data class CreateBarcodeResponse(
  @Schema(description = "The generated barcode", example = "123456789012")
  val barcode: String,
)

data class CheckBarcodeRequest(
  @Schema(description = "The barcode being checked", example = "123456789012", required = true)
  val barcode: String,
)

data class CheckBarcodeResponse(
  @Schema(description = "The organisation that created the barcode", example = "Aardvark Solicitors", required = true)
  val createdBy: String,
)
