package uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.config

import mu.KotlinLogging
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatus.BAD_REQUEST
import org.springframework.http.HttpStatus.FORBIDDEN
import org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR
import org.springframework.http.HttpStatus.NOT_FOUND
import org.springframework.http.HttpStatus.UNAUTHORIZED
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.access.AuthorizationServiceException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.client.RestClientException
import org.springframework.web.client.RestClientResponseException
import javax.persistence.EntityNotFoundException
import javax.validation.ValidationException

private val log = KotlinLogging.logger {}

@RestControllerAdvice
class SendLegalMailToPrisonsApiExceptionHandler {

  @ExceptionHandler(HttpMessageNotReadableException::class)
  fun handleJsonParseException(e: HttpMessageNotReadableException): ResponseEntity<ErrorResponse> {
    log.info("Request message unreadable exception: {}", e.message)
    return ResponseEntity
      .status(BAD_REQUEST)
      .body(
        ErrorResponse(
          status = BAD_REQUEST.value(),
          userMessage = "Failed to read the payload}",
          developerMessage = e.message
        )
      )
  }

  @ExceptionHandler(AccessDeniedException::class)
  fun handleAccessDeniedException(e: AccessDeniedException): ResponseEntity<ErrorResponse> {
    log.info("Access denied exception: {}", e.message)
    return ResponseEntity
      .status(FORBIDDEN)
      .body(
        ErrorResponse(
          status = FORBIDDEN.value(),
          userMessage = "Authentication problem. Check token.",
          developerMessage = e.message
        )
      )
  }

  @ExceptionHandler(AuthorizationServiceException::class)
  fun handleAuthorizationServiceException(e: AuthorizationServiceException): ResponseEntity<ErrorResponse> {
    log.info("Auth service exception: {}", e.message)
    return ResponseEntity
      .status(UNAUTHORIZED)
      .body(
        ErrorResponse(
          status = UNAUTHORIZED.value(),
          userMessage = "Authorization problem. Check roles.",
          developerMessage = e.message
        )
      )
  }

  @ExceptionHandler(RestClientResponseException::class)
  fun handleRestClientException(e: RestClientResponseException): ResponseEntity<ErrorResponse> {
    log.error("RestClientResponseException: {}", e.message)
    return ResponseEntity
      .status(e.rawStatusCode)
      .body(
        ErrorResponse(
          status = e.rawStatusCode,
          userMessage = "Error calling downstream service.",
          developerMessage = e.message
        )
      )
  }

  @ExceptionHandler(RestClientException::class)
  fun handleRestClientException(e: RestClientException): ResponseEntity<ErrorResponse> {
    log.error("RestClientException: {}", e.message)
    return ResponseEntity
      .status(INTERNAL_SERVER_ERROR)
      .body(
        ErrorResponse(
          status = INTERNAL_SERVER_ERROR.value(),
          userMessage = "Error calling downstream service.",
          developerMessage = e.message
        )
      )
  }

  @ExceptionHandler(EntityNotFoundException::class)
  fun handleEntityNotFoundException(e: EntityNotFoundException): ResponseEntity<ErrorResponse> {
    log.info("Entity not found exception: {}", e.message)
    return ResponseEntity
      .status(NOT_FOUND)
      .body(
        ErrorResponse(
          status = NOT_FOUND.value(),
          userMessage = "Not found.}",
          developerMessage = e.message
        )
      )
  }

  @ExceptionHandler(ValidationException::class)
  fun handleValidationException(e: ValidationException): ResponseEntity<ErrorResponse> {
    log.info("Validation exception: {}", e.message)
    return ResponseEntity
      .status(BAD_REQUEST)
      .body(
        ErrorResponse(
          status = BAD_REQUEST,
          userMessage = "Validation failure.",
          developerMessage = e.message
        )
      )
  }

  @ExceptionHandler(java.lang.Exception::class)
  fun handleException(e: java.lang.Exception): ResponseEntity<ErrorResponse?>? {
    log.error("Unexpected exception", e)
    return ResponseEntity
      .status(INTERNAL_SERVER_ERROR)
      .body(
        ErrorResponse(
          status = INTERNAL_SERVER_ERROR,
          userMessage = "Unexpected error: ${e.message}",
          developerMessage = e.message
        )
      )
  }
}

data class ErrorResponse(
  val status: Int,
  val errorCode: Int? = null,
  val userMessage: String? = null,
  val developerMessage: String? = null,
  val moreInfo: String? = null
) {
  constructor(
    status: HttpStatus,
    errorCode: Int? = null,
    userMessage: String? = null,
    developerMessage: String? = null,
    moreInfo: String? = null
  ) :
    this(status.value(), errorCode, userMessage, developerMessage, moreInfo)
}
