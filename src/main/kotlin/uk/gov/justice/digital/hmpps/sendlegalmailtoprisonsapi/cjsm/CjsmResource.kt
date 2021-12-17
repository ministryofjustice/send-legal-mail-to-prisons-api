package uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.cjsm

import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping(produces = [MediaType.APPLICATION_JSON_VALUE])
class CjsmResource(private val cjsmService: CjsmService) {

  @PostMapping(value = ["/cjsm/directory/refresh"])
  fun refreshCjsmDirectory() = cjsmService.saveCjsmDirectoryCsv()
}
