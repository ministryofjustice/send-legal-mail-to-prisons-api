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
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.config.ErrorResponse
import java.time.LocalDate
import javax.validation.Valid
import javax.validation.constraints.Pattern

@RestController
@RequestMapping(produces = [MediaType.APPLICATION_JSON_VALUE])
class ContactResource(private val contactService: ContactService) {

  @PostMapping(value = ["/contact"])
  @ResponseBody
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize("hasRole('ROLE_SLM_CREATE_BARCODE')")
  @Operation(
    summary = "Creates a new Contact for the logged in user",
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
        description = "Unauthorised, requires a valid magic link token",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "409",
        description = "Conflict, the specified new contact already exists for this user",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ]
  )
  fun createContact(@Valid @RequestBody createContactRequest: CreateContactRequest, authentication: Authentication): ContactResponse =
    contactService.createContact(authentication.name, createContactRequest).let {
      return ContactResponse(
        id = it.id!!,
        prisonerName = it.name,
        prisonId = it.prisonCode,
        dob = it.dob,
        prisonNumber = it.prisonNumber
      )
    }

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
        responseCode = "401",
        description = "Unauthorised, requires a valid magic link token",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      )
    ]
  )
  fun searchContactsByName(@RequestParam name: String): Collection<ContactResource> {
    TODO()
  }
}

@ContactHasDobOrPrisonNumber
data class CreateContactRequest(
  @Schema(description = "The name of the new contact", example = "John Doe", required = true)
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
