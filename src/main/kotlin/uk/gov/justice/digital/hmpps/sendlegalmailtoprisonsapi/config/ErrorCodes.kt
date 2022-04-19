package uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.config

import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.magiclink.MAX_EMAIL_LENGTH
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Schema(subTypes = [StandardErrorCodes::class, AuthenticationRequestErrorCodes::class, CheckBarcodeErrorCodes::class])
sealed class ErrorCode(
  @Schema(description = "The error code", example = "ERROR_IDENTIFIER")
  val code: String,
  @Schema(description = "A human readable description of the error", example = "An error occurred")
  val userMessage: String
)

@Schema(oneOf = [AuthenticationError::class, DownstreamError::class, InternalError::class, MalformedRequest::class, NotFound::class])
sealed class StandardErrorCodes(
  @Schema(allowableValues = ["AUTH", "DOWNSTREAM", "INTERNAL_ERROR", "MALFORMED_REQUEST", "NOT_FOUND"])
  code: String,
  userMessage: String
) : ErrorCode(code, userMessage)
object AuthenticationError : StandardErrorCodes("AUTH", "Authentication failure")
object DownstreamError : StandardErrorCodes("DOWNSTREAM", "An error occurred calling a downstream service")
object InternalError : StandardErrorCodes("INTERNAL_ERROR", "An unexpected error occurred")
object MalformedRequest : StandardErrorCodes("MALFORMED_REQUEST", "Failed to read the payload")
object NotFound : StandardErrorCodes("NOT_FOUND", "Not found")

@Schema(oneOf = [SessionIdMandatory::class, EmailMandatory::class, EmailTooLong::class, EmailInvalid::class, EmailInvalidCjsm::class])
sealed class AuthenticationRequestErrorCodes(
  @Schema(allowableValues = ["SESSION_ID_MANDATORY", "EMAIL_MANDATORY", "EMAIL_TOO_LONG", "INVALID_EMAIL", "INVALID_CJSM_EMAIL"])
  code: String,
  userMessage: String
) : StandardErrorCodes(code, userMessage)
object SessionIdMandatory : AuthenticationRequestErrorCodes("SESSION_ID_MANDATORY", "The session ID must be entered")
object EmailMandatory : AuthenticationRequestErrorCodes("EMAIL_MANDATORY", "The email address must be entered")
object EmailTooLong : AuthenticationRequestErrorCodes("EMAIL_TOO_LONG", "The email address can have a maximum length of $MAX_EMAIL_LENGTH")
object EmailInvalid : AuthenticationRequestErrorCodes("INVALID_EMAIL", "Enter an email address in the correct format")
object EmailInvalidCjsm : AuthenticationRequestErrorCodes("INVALID_CJSM_EMAIL", "Enter an email address which ends with 'cjsm.net'")

@Schema(oneOf = [OneTimeCodeSessionNotFound::class, OneTimeCodeTooManyAttempts::class])
sealed class OneTimeCodeErrorCode(
  @Schema(allowableValues = ["OTC_SESSION_NOT_FOUND", "OTC_TOO_MANY_ATTEMPTS", "OTC_NOT_FOUND"])
  code: String,
  userMessage: String
) : StandardErrorCodes(code, userMessage)
object OneTimeCodeSessionNotFound : OneTimeCodeErrorCode("OTC_SESSION_NOT_FOUND", "There is no one time code saved for the session")
object OneTimeCodeTooManyAttempts : OneTimeCodeErrorCode("OTC_TOO_MANY_ATTEMPTS", "Too many incorrect one time codes have been attempted")
object OneTimeCodeNotFound : OneTimeCodeErrorCode("OTC_NOT_FOUND", "The one time code could not be found")

@Schema(oneOf = [DuplicateContact::class])
sealed class ContactErrorCodes(
  @Schema(allowableValues = ["DUPLICATE_CONTACT"])
  code: String,
  userMessage: String
) : StandardErrorCodes(code, userMessage)
object DuplicateContact : ContactErrorCodes("CONFLICT", "A contact already exists for this prison number")

@Schema(oneOf = [Duplicate::class, Expired::class, RandomCheck::class])
sealed class CheckBarcodeErrorCodes(
  @Schema(allowableValues = ["DUPLICATE", "EXPIRED", "RANDOM_CHECK"])
  code: String,
  userMessage: String
) : StandardErrorCodes(code, userMessage)

class Duplicate(
  @Schema(description = "The time that the original barcode was scanned", example = "2021-11-30T09:06:10Z")
  val scannedDate: Instant,
  @Schema(description = "The prison where the original barcode was scanned", example = "MDI")
  val scannedLocation: String,
  @Schema(description = "The organisation that created the barcode in the first place", example = "Aardvark Solicitors")
  val createdBy: String,
  @Schema(description = "The original barcode recipient name", example = "John Smith")
  val recipientName: String,
  @Schema(description = "The original barcode recipient prison number", example = "A1234BC")
  val recipientPrisonNumber: String?,
  @Schema(description = "The original barcode recipient date of birth", example = "1990-01-02")
  val recipientDob: LocalDate?,
) : CheckBarcodeErrorCodes("DUPLICATE", "Someone scanned this barcode ${scannedDate.formatAtTimeOnDate()} at $scannedLocation. It was originally addressed to $recipientName, ${recipientPrisonNumber ?: DateTimeFormatter.ISO_DATE.format(recipientDob)}. It may be an illegal copy.")

class Expired(
  @Schema(description = "The time the barcode was created", example = "2021-11-30T09:06:10Z")
  val createdDate: Instant,
  @Schema(description = "The number of days before a barcode expires", example = "28")
  val barcodeExpiryDays: Long,
  @Schema(description = "The organisation that created the barcode in the first place", example = "Aardvark Solicitors")
  val createdBy: String
) : CheckBarcodeErrorCodes("EXPIRED", "This barcode was created ${createdDate.ageInDays()}, ${createdDate.formatOnDate()}.")

class RandomCheck(
  @Schema(description = "The organisation that created the barcode in the first place", example = "Aardvark Solicitors")
  val createdBy: String
) : CheckBarcodeErrorCodes("RANDOM_CHECK", "For additional security this barcode has been selected for a random check")

private val timeFormatter = DateTimeFormatter.ofPattern("h:mm a").withZone(ZoneId.systemDefault())
private val dateFormatter = DateTimeFormatter.ofPattern("d MMMM y").withZone(ZoneId.systemDefault())
private fun Instant.formatAtTimeOnDate() = """at ${timeFormatter.format(this).lowercase()} ${this.formatOnDate()}"""
private fun Instant.formatOnDate() = """on ${dateFormatter.format(this)}"""
private fun Instant.ageInDays() = """${Duration.between(this, Instant.now()).toDays()} days ago"""
