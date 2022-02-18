package uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.contact

import com.fasterxml.jackson.annotation.JsonFormat
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.config.ErrorResponse
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.config.ResourceNotFoundException
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.validators.validateRequestHasDobOrPrisonNumber
import java.time.LocalDate
import javax.validation.Valid
import javax.validation.constraints.Pattern
import javax.validation.constraints.Size

@RestController
@RequestMapping(produces = [MediaType.APPLICATION_JSON_VALUE])
class ContactResource(private val contactService: ContactService) {

  @PostMapping(value = ["/contact"])
  @ResponseBody
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize("hasRole('ROLE_SLM_CREATE_BARCODE')")
  @Operation(
    summary = "Creates a new Contact for the signed in user",
    security = [SecurityRequirement(name = "ROLE_SLM_CREATE_BARCODE")]
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "201",
        description = "Contact created",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ContactResponse::class))],
      ),
      ApiResponse(
        responseCode = "400",
        description = "Bad request",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
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
      ApiResponse(
        responseCode = "409",
        description = "Conflict, the specified new contact already exists for this user. See ContactErrorCodes.",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ]
  )
  fun createContact(@Valid @RequestBody contactRequest: ContactRequest, authentication: Authentication): ContactResponse {
    validateRequestHasDobOrPrisonNumber(contactRequest)
    return contactService.createContact(authentication.name, contactRequest)
  }

  @PutMapping(value = ["/contact/{id}"])
  @ResponseBody
  @ResponseStatus(HttpStatus.OK)
  @PreAuthorize("hasRole('ROLE_SLM_CREATE_BARCODE')")
  @Operation(
    summary = "Update an existing Contact for the signed in user",
    security = [SecurityRequirement(name = "ROLE_SLM_CREATE_BARCODE")]
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Contact udpated",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ContactResponse::class))],
      ),
      ApiResponse(
        responseCode = "400",
        description = "Bad request",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
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
      ApiResponse(
        responseCode = "404",
        description = "Contact not found",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "409",
        description = "Conflict, the specified new contact already exists for this user. See ContactErrorCodes.",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ]
  )
  fun updateContact(
    @Valid @RequestBody contactRequest: ContactRequest,
    @PathVariable id: Long,
    authentication: Authentication
  ): ContactResponse {
    validateRequestHasDobOrPrisonNumber(contactRequest)
    return contactService.updateContact(authentication.name, id, contactRequest)
      ?: throw ResourceNotFoundException("Contact not found")
  }

  @GetMapping(value = ["/contact/{prisonNumber}"])
  @ResponseBody
  @ResponseStatus(HttpStatus.OK)
  @PreAuthorize("hasRole('ROLE_SLM_CREATE_BARCODE')")
  @Operation(
    summary = "Retrieve a Contact by prisonNumber for the logged in user",
    security = [SecurityRequirement(name = "ROLE_SLM_CREATE_BARCODE")],
    parameters = [Parameter(`in` = ParameterIn.PATH, name = "prisonNumber", example = "A1234BC", description = "The prison number of the Contact to return.")]
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Contact returned",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ContactResponse::class))],
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorised, requires a valid magic link token",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Forbidden, requires a valid token with role ROLE_SLM_CREATE_BARCODE",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "404",
        description = "Contact not found",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ]
  )
  fun getContactByPrisonNumber(@PathVariable prisonNumber: String, authentication: Authentication): ContactResponse =
    contactService.getContactByPrisonNumber(authentication.name, prisonNumber)
      ?: throw ResourceNotFoundException("Could not find a matching Contact [${authentication.name}, $prisonNumber]")

  @GetMapping(value = ["/contacts"])
  @ResponseBody
  @ResponseStatus(HttpStatus.OK)
  @PreAuthorize("hasRole('ROLE_SLM_CREATE_BARCODE')")
  @Operation(
    summary = "Search for Contacts by their name or partial name",
    security = [SecurityRequirement(name = "ROLE_SLM_CREATE_BARCODE")],
    parameters = [Parameter(`in` = ParameterIn.QUERY, name = "name", example = "john", description = "The name or partial name of the Contacts to return. Case insensitive.")]
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Matching Contacts",
        content = [Content(mediaType = "application/json", array = ArraySchema(schema = Schema(implementation = ContactResponse::class)))],
      ),
      ApiResponse(
        responseCode = "400",
        description = "Bad request",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
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
  fun searchContactsByName(
    @RequestParam(required = true) name: String,
    authentication: Authentication
  ): Collection<ContactResponse> {
    return contactService.searchContactsByName(authentication.name, name)
  }
}

data class ContactRequest(
  @Schema(description = "The name of the new contact", example = "John Doe", required = true)
  @field:Pattern(regexp = "^[a-zA-Z '`-]+$")
  @field:Size(max = 60)
  val prisonerName: String,

  @Schema(description = "The ID of the prison location of the new contact", example = "BXI", required = true)
  @field:Pattern(regexp = "^[A-Z]{3}$")
  val prisonId: String,

  @Schema(description = "The date of birth of the new contact if known", example = "1965-04-23", required = false)
  @field:JsonFormat(pattern = "yyyy-MM-dd")
  val dob: LocalDate? = null,

  @Schema(description = "The prison number of the new contact if known", example = "A1234BC", required = false)
  @field:Pattern(regexp = "^[A-Z]\\d{4}[A-Z]{2}$")
  val prisonNumber: String? = null,
)

data class ContactResponse(
  @Schema(description = "The ID of the contact", example = "1", required = true)
  val id: Long,

  @Schema(description = "The name of the contact", example = "John Doe", required = true)
  val prisonerName: String,

  @Schema(description = "The ID of the prison location of the contact", example = "BXI", required = true)
  val prisonId: String,

  @Schema(description = "The date of birth of the contact if known", example = "1965-04-23", required = false)
  @JsonFormat(pattern = "yyyy-MM-dd")
  val dob: LocalDate? = null,

  @Schema(description = "The prison number of the contact if known", example = "A1234BC", required = false)
  val prisonNumber: String? = null
)
