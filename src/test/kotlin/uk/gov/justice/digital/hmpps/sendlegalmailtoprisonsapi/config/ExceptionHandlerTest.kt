package uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.config

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Instant

class ExceptionHandlerTest {

  @Test
  fun `should create a duplicate validation exception`() {
    val ex = Duplicate(Instant.parse("2021-12-08T09:11:23Z"), "some_location")

    assertThat(ex.code).isEqualTo("DUPLICATE")
    assertThat(ex.userMessage).contains("at 9:11 am on 8 December 2021")
  }
}
