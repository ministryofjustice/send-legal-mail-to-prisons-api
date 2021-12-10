package uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.config

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

class ErrorCodesTest {

  @Nested
  inner class Duplicate {
    @Test
    fun `should format the user message correctly`() {
      val ex = Duplicate(Instant.parse("2021-12-08T09:11:23Z"), "some_location")

      assertThat(ex.userMessage).contains("at 9:11 am on 8 December 2021")
    }
  }

  @Nested
  inner class Expired {
    @Test
    fun `should format the user message correctly`() {
      val createdTime = Instant.now().minus(55, ChronoUnit.DAYS)
      val createdTimeString =
        DateTimeFormatter.ofPattern("d MMMM y").withZone(ZoneId.systemDefault()).format(createdTime)
      val ex = Expired(createdTime, 28)

      assertThat(ex.userMessage).contains("55 days ago")
      assertThat(ex.userMessage).contains(createdTimeString)
    }
  }
}
