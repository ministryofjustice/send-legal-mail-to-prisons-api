package uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.config

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

class ExceptionHandlerTest {

  @Test
  fun `should create a duplicate validation exception`() {
    val ex = Duplicate(Instant.parse("2021-12-08T09:11:23Z"), "some_location")

    assertThat(ex.code).isEqualTo("DUPLICATE")
    assertThat(ex.userMessage).contains("at 9:11 am on 8 December 2021")
  }

  @Test
  fun `should create an expired validation exception`() {
    val createdTime = Instant.now().minus(55, ChronoUnit.DAYS)
    val createdTimeString = DateTimeFormatter.ofPattern("d MMMM y").withZone(ZoneId.systemDefault()).format(createdTime)
    val ex = Expired(createdTime, Duration.ofDays(28))

    assertThat(ex.code).isEqualTo("EXPIRED")
    assertThat(ex.userMessage).contains("55 days ago")
    assertThat(ex.userMessage).contains(createdTimeString)
  }
}
