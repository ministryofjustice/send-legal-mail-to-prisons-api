package uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.config

import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.magiclink.MAX_EMAIL_LENGTH
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

sealed class ErrorCode(val code: String, val userMessage: String)

sealed class StandardErrorCodes(code: String, userMessage: String) : ErrorCode(code, userMessage)
object AuthenticationError : StandardErrorCodes("AUTH", "Authentication failure")
object DownstreamError : StandardErrorCodes("DOWNSTREAM", "An error occurred calling a downstream service")
object InternalError : StandardErrorCodes("INTERNAL_ERROR", "An unexpected error occurred")
object MalformedRequest : StandardErrorCodes("MALFORMED_REQUEST", "Failed to read the payload")
object NotFound : StandardErrorCodes("NOT_FOUND", "Not found")

sealed class MagicLinkRequestErrorCodes(code: String, userMessage: String) : StandardErrorCodes(code, userMessage)
object EmailMandatory : MagicLinkRequestErrorCodes("EMAIL_MANDATORY", "The email address must be entered")
object EmailTooLong : MagicLinkRequestErrorCodes("EMAIL_TOO_LONG", "The email address can have a maximum length of $MAX_EMAIL_LENGTH")
object EmailInvalid : MagicLinkRequestErrorCodes("INVALID_EMAIL", "Enter an email address in the correct format")
object EmailInvalidCjsm : MagicLinkRequestErrorCodes("INVALID_CJSM_EMAIL", "Enter an email address which ends with 'cjsm.net'")

sealed class CheckBarcodeErrorCodes(code: String, userMessage: String) : StandardErrorCodes(code, userMessage)
class Duplicate(val scannedDate: Instant, val scannedLocation: String, val createdBy: String) :
  CheckBarcodeErrorCodes("DUPLICATE", "Someone scanned this barcode ${scannedDate.formatAtTimeOnDate()} at $scannedLocation. It may be an illegal copy.")
class Expired(val createdDate: Instant, val barcodeExpiryDays: Long, val createdBy: String) :
  CheckBarcodeErrorCodes("EXPIRED", "This barcode was created ${createdDate.ageInDays()}, ${createdDate.formatOnDate()}.")
class RandomCheck(val createdBy: String) :
  CheckBarcodeErrorCodes("RANDOM_CHECK", "For additional security this barcode has been selected for a random check")

private val timeFormatter = DateTimeFormatter.ofPattern("h:mm a").withZone(ZoneId.systemDefault())
private val dateFormatter = DateTimeFormatter.ofPattern("d MMMM y").withZone(ZoneId.systemDefault())
private fun Instant.formatAtTimeOnDate() = """at ${timeFormatter.format(this).lowercase()} ${this.formatOnDate()}"""
private fun Instant.formatOnDate() = """on ${dateFormatter.format(this)}"""
private fun Instant.ageInDays() = """${Duration.between(this, Instant.now()).toDays()} days ago"""
