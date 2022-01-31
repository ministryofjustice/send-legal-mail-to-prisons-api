package uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.config

import io.swagger.v3.oas.annotations.media.Schema
import mu.KotlinLogging
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatus.BAD_REQUEST
import org.springframework.http.HttpStatus.FORBIDDEN
import org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR
import org.springframework.http.HttpStatus.NOT_FOUND
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.security.access.AccessDeniedException
import org.springframework.web.bind.MethodArgumentNotValidException
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

  @ExceptionHandler(MethodArgumentNotValidException::class)
  fun handleMethodArgumentNotValidException(e: MethodArgumentNotValidException): ResponseEntity<ErrorResponse> {
    log.info("Method argument not valid exception: {}", e.message)
    return ResponseEntity
      .status(BAD_REQUEST)
      .body(ErrorResponse(status = BAD_REQUEST, errorCode = MalformedRequest))
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

class ErrorResponse(
  @Schema(description = "The HTTP status code", example = "400")
  val status: Int,
  @Schema(description = "The error code describing the error")
  val errorCode: ErrorCode
) {
  constructor(status: HttpStatus, errorCode: ErrorCode) : this(status.value(), errorCode)
}
