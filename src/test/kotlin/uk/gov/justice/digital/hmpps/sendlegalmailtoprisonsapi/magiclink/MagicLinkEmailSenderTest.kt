package uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.magiclink

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.check
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.mail.SimpleMailMessage
import org.springframework.mail.javamail.JavaMailSender
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.config.MagicLinkConfig
import java.time.Duration
import java.time.temporal.ChronoUnit

class MagicLinkEmailSenderTest {

  private val mockMailSender: JavaMailSender = mock()
  private val magicLinkEmailSender = MagicLinkEmailSender(MagicLinkConfig(Duration.of(0, ChronoUnit.SECONDS), "some-url"), mockMailSender)

  @Test
  fun `The mail sender should be called to send the mail`() {
    magicLinkEmailSender.send("any-email", "any-secret")

    verify(mockMailSender).send(any<SimpleMailMessage>())
  }

  @Test
  fun `The mail should be sent to the passed email`() {
    magicLinkEmailSender.send("an.email@company.com", "any-secret")

    verify(mockMailSender).send(
      check<SimpleMailMessage> {
        assertThat(it.to).isEqualTo(arrayOf("an.email@company.com"))
      }
    )
  }

  @Test
  fun `The mail should contain the magic link`() {
    magicLinkEmailSender.send("any-email", "this-is-a-secret")

    verify(mockMailSender).send(
      check<SimpleMailMessage> {
        assertThat(it.text).contains("some-url?secret=this-is-a-secret")
      }
    )
  }
}
