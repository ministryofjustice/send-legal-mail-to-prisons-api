package uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.config

import mu.KotlinLogging
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatus.BAD_REQUEST
import org.springframework.http.HttpStatus.FORBIDDEN
import org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR
import org.springframework.http.HttpStatus.NOT_FOUND
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.security.access.AccessDeniedException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.client.RestClientException
import org.springframework.web.client.RestClientResponseException
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.magiclink.MAX_EMAIL_LENGTH
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.persistence.EntityNotFoundException

private val log = KotlinLogging.logger {}

@RestControllerAdvice
class SendLegalMailToPrisonsApiExceptionHandler {

  @ExceptionHandler(HttpMessageNotReadableException::class)
  fun handleJsonParseException(e: HttpMessageNotReadableException): ResponseEntity<ErrorResponse> {
    log.info("Request message unreadable exception: {}", e.message)
    return ResponseEntity
      .status(BAD_REQUEST)
      .body(ErrorResponse(status = BAD_REQUEST, errorCode = MalformedRequest))
  }

  @ExceptionHandler(AccessDeniedException::class)
  fun handleAccessDeniedException(e: AccessDeniedException): ResponseEntity<ErrorResponse> {
    log.info("Access denied exception: {}", e.message)
    return ResponseEntity
      .status(FORBIDDEN)
      .body(ErrorResponse(status = FORBIDDEN, errorCode = AuthenticationError))
  }

  @ExceptionHandler(RestClientResponseException::class)
  fun handleRestClientException(e: RestClientResponseException): ResponseEntity<ErrorResponse> {
    log.error("RestClientResponseException: {}", e.message)
    return ResponseEntity
      .status(e.rawStatusCode)
      .body(ErrorResponse(status = e.rawStatusCode, errorCode = DownstreamError))
  }

  @ExceptionHandler(RestClientException::class)
  fun handleRestClientException(e: RestClientException): ResponseEntity<ErrorResponse> {
    log.error("RestClientException: {}", e.message)
    return ResponseEntity
      .status(INTERNAL_SERVER_ERROR)
      .body(ErrorResponse(status = INTERNAL_SERVER_ERROR, errorCode = DownstreamError))
  }

  @ExceptionHandler(EntityNotFoundException::class)
  fun handleEntityNotFoundException(e: EntityNotFoundException): ResponseEntity<ErrorResponse> {
    log.info("Entity not found exception: {}", e.message)
    return ResponseEntity
      .status(NOT_FOUND)
      .body(ErrorResponse(status = NOT_FOUND, errorCode = NotFound))
  }

  @ExceptionHandler(ValidationException::class)
  fun handleValidationException(e: ValidationException): ResponseEntity<ErrorResponse> {
    log.info("Validation exception: {}", e.message)
    return ResponseEntity
      .status(BAD_REQUEST)
      .body(ErrorResponse(status = BAD_REQUEST, errorCode = e.errorCode))
  }

  @ExceptionHandler(Exception::class)
  fun handleException(e: Exception): ResponseEntity<ErrorResponse?>? {
    log.error("Unexpected exception", e)
    return ResponseEntity
      .status(INTERNAL_SERVER_ERROR)
      .body(ErrorResponse(status = INTERNAL_SERVER_ERROR, errorCode = InternalError))
  }
}

class ValidationException(val errorCode: ErrorCode) : RuntimeException()

class ErrorResponse(val status: Int, val errorCode: ErrorCode) {
  constructor(status: HttpStatus, errorCode: ErrorCode) : this(status.value(), errorCode)
}

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
class Duplicate(val scannedDate: Instant, val scannedLocation: String) : CheckBarcodeErrorCodes("DUPLICATE", "Someone scanned this barcode ${scannedDate.format()} at $scannedLocation. It may be an illegal copy.")

private val timeFormatter = DateTimeFormatter.ofPattern("h:mm a").withZone(ZoneId.systemDefault())
private val dateFormatter = DateTimeFormatter.ofPattern("d MMMM y").withZone(ZoneId.systemDefault())
private fun Instant.format(): String {
  return """at ${timeFormatter.format(this).lowercase()} on ${dateFormatter.format(this)}"""
}
