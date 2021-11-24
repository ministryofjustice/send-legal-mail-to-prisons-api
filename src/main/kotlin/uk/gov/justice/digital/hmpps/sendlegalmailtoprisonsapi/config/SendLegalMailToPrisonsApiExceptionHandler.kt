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
import javax.persistence.EntityNotFoundException

private val log = KotlinLogging.logger {}

@RestControllerAdvice
class SendLegalMailToPrisonsApiExceptionHandler {

  @ExceptionHandler(HttpMessageNotReadableException::class)
  fun handleJsonParseException(e: HttpMessageNotReadableException): ResponseEntity<ErrorResponse> {
    log.info("Request message unreadable exception: {}", e.message)
    return ResponseEntity
      .status(BAD_REQUEST)
      .body(ErrorResponse(status = BAD_REQUEST, errorCode = ErrorCode.MALFORMED_REQUEST))
  }

  @ExceptionHandler(AccessDeniedException::class)
  fun handleAccessDeniedException(e: AccessDeniedException): ResponseEntity<ErrorResponse> {
    log.info("Access denied exception: {}", e.message)
    return ResponseEntity
      .status(FORBIDDEN)
      .body(ErrorResponse(status = FORBIDDEN, errorCode = ErrorCode.AUTH))
  }

  @ExceptionHandler(RestClientResponseException::class)
  fun handleRestClientException(e: RestClientResponseException): ResponseEntity<ErrorResponse> {
    log.error("RestClientResponseException: {}", e.message)
    return ResponseEntity
      .status(e.rawStatusCode)
      .body(ErrorResponse(status = e.rawStatusCode, errorCode = ErrorCode.DOWNSTREAM))
  }

  @ExceptionHandler(RestClientException::class)
  fun handleRestClientException(e: RestClientException): ResponseEntity<ErrorResponse> {
    log.error("RestClientException: {}", e.message)
    return ResponseEntity
      .status(INTERNAL_SERVER_ERROR)
      .body(ErrorResponse(status = INTERNAL_SERVER_ERROR, errorCode = ErrorCode.DOWNSTREAM))
  }

  @ExceptionHandler(EntityNotFoundException::class)
  fun handleEntityNotFoundException(e: EntityNotFoundException): ResponseEntity<ErrorResponse> {
    log.info("Entity not found exception: {}", e.message)
    return ResponseEntity
      .status(NOT_FOUND)
      .body(ErrorResponse(status = NOT_FOUND, errorCode = ErrorCode.NOT_FOUND))
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
      .body(ErrorResponse(status = INTERNAL_SERVER_ERROR, errorCode = ErrorCode.INTERNAL_ERROR))
  }
}

class ValidationException(val errorCode: ErrorCode) : RuntimeException()

class ErrorResponse(val status: Int, errorCode: ErrorCode) {
  constructor(status: HttpStatus, errorCode: ErrorCode) : this(status.value(), errorCode)

  val errorCode = errorCode.name
  val userMessage = errorCode.userMessage
}

enum class ErrorCode(val userMessage: String) {
  // Standard errors
  AUTH("Authentication failure"),
  DOWNSTREAM("An error occurred calling a downstream service"),
  INTERNAL_ERROR("An unexpected error occurred"),
  MALFORMED_REQUEST("Failed to read the payload"),
  NOT_FOUND("Not found"),
  // Custom errors
  EMAIL_MANDATORY("The email address must be entered"),
  INVALID_EMAIL("Enter an email address in the correct format"),
  INVALID_CJSM_EMAIL("Enter an email address which ends with 'cjsm.net'"),
}
