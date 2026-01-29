package uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.config

import io.swagger.v3.oas.annotations.media.Schema
import mu.KotlinLogging
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatus.BAD_REQUEST
import org.springframework.http.HttpStatus.CONFLICT
import org.springframework.http.HttpStatus.FORBIDDEN
import org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR
import org.springframework.http.HttpStatus.NOT_FOUND
import org.springframework.http.HttpStatus.UNAUTHORIZED
import org.springframework.http.HttpStatusCode
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.security.access.AccessDeniedException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.MissingServletRequestParameterException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.client.RestClientException
import org.springframework.web.client.RestClientResponseException
import org.springframework.web.context.request.WebRequest
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler

private val log = KotlinLogging.logger {}

@RestControllerAdvice
class SendLegalMailToPrisonsApiExceptionHandler : ResponseEntityExceptionHandler() {
  override fun handleHttpMessageNotReadable(e: HttpMessageNotReadableException, headers: HttpHeaders, status: HttpStatusCode, request: WebRequest): ResponseEntity<Any> {
    log.info("Request message unreadable exception: {}", e.message)
    return ResponseEntity
      .status(BAD_REQUEST)
      .body(ErrorResponse(status = BAD_REQUEST, errorCode = MalformedRequest))
  }

  override fun handleMethodArgumentNotValid(e: MethodArgumentNotValidException, headers: HttpHeaders, status: HttpStatusCode, request: WebRequest): ResponseEntity<Any> {
    log.info("Method argument not valid exception: {}", e.message)
    return ResponseEntity
      .status(BAD_REQUEST)
      .body(ErrorResponse(status = BAD_REQUEST, errorCode = MalformedRequest))
  }

  override fun handleMissingServletRequestParameter(e: MissingServletRequestParameterException, headers: HttpHeaders, status: HttpStatusCode, request: WebRequest): ResponseEntity<Any> {
    log.info { "Missing required querystring parameter '${e.parameterName}' on request" }
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
      .status(e.statusCode)
      .body(ErrorResponse(status = e.statusCode.value(), errorCode = DownstreamError))
  }

  @ExceptionHandler(RestClientException::class)
  fun handleRestClientException(e: RestClientException): ResponseEntity<ErrorResponse> {
    log.error("RestClientException: {}", e.message)
    return ResponseEntity
      .status(INTERNAL_SERVER_ERROR)
      .body(ErrorResponse(status = INTERNAL_SERVER_ERROR, errorCode = DownstreamError))
  }

  @ExceptionHandler(ResourceNotFoundException::class)
  fun handleResourceNotFoundException(e: ResourceNotFoundException): ResponseEntity<ErrorResponse> {
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

  @ExceptionHandler(DuplicateContactException::class)
  fun handleDuplicateContactException(e: DuplicateContactException): ResponseEntity<ErrorResponse> {
    log.info { "Duplicate Contact exception: [${e.userId}, ${e.prisonNumber}]" }
    return ResponseEntity
      .status(CONFLICT)
      .body(ErrorResponse(status = CONFLICT, errorCode = DuplicateContact))
  }

  @ExceptionHandler(OneTimeCodeException::class)
  fun handleException(e: OneTimeCodeException): ResponseEntity<ErrorResponse>? {
    log.info("Failed attempt to resolve a one time code", e)
    return ResponseEntity
      .status(UNAUTHORIZED)
      .body(ErrorResponse(status = UNAUTHORIZED, errorCode = e.errorCode))
  }

  @ExceptionHandler(Exception::class)
  fun handleException(e: Exception): ResponseEntity<ErrorResponse>? {
    log.error("Unexpected exception", e)
    return ResponseEntity
      .status(INTERNAL_SERVER_ERROR)
      .body(ErrorResponse(status = INTERNAL_SERVER_ERROR, errorCode = InternalError))
  }
}

class ValidationException(val errorCode: ErrorCode) : RuntimeException(errorCode.code)

class DuplicateContactException(val userId: String, val prisonNumber: String) : RuntimeException()

class ResourceNotFoundException(message: String) : RuntimeException(message)

class OneTimeCodeException(val errorCode: OneTimeCodeErrorCode) : RuntimeException(errorCode.code)

class ErrorResponse(
  @Schema(description = "The HTTP status code", example = "400")
  val status: Int,
  @Schema(description = "The error code describing the error")
  val errorCode: ErrorCode,
) {
  constructor(status: HttpStatus, errorCode: ErrorCode) : this(status.value(), errorCode)
}
