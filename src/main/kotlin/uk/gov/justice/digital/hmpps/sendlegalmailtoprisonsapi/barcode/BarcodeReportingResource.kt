package uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.barcode

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
class BarcodeReportingResource(private val barcodeReportingService: BarcodeReportingService) {

  @PostMapping("/barcode-stats-report")
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(summary = "Generates the latest barcode statistics report and sends to configured users")
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "201",
        description = "Report created and emailed"
      ), ApiResponse(
        responseCode = "404",
        description = "No recipients configured. Add a comma separated list of email addresses to helm values file entry env.APP_BARCODE_STATS_REPORT_RECIPIENT_EMAILS."
      )
    ]
  )
  fun createBarcodeStatsReport() = barcodeReportingService.distributeBarcodeStats()
}
