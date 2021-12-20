package uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.cjsm

import io.swagger.v3.oas.annotations.Hidden
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Hidden
@RestController
@RequestMapping(produces = [MediaType.APPLICATION_JSON_VALUE])
class CjsmResource(private val cjsmService: CjsmService) {

  @PostMapping(value = ["/cjsm/directory/refresh"])
  fun refreshCjsmDirectory() = cjsmService.saveCjsmDirectoryCsv()
}
