package uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.magiclink

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.check
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.springframework.mail.SimpleMailMessage
import org.springframework.mail.javamail.JavaMailSender
import java.time.Duration
import java.time.temporal.ChronoUnit

class MagicLinkEmailSenderTest {

  private val mockMailSender: JavaMailSender = mock()
  private val magicLinkEmailSender = MagicLinkEmailSender(MagicLinkConfig(Duration.of(0, ChronoUnit.SECONDS), "some-url"), mockMailSender)

  @Test
  fun `The mail should be sent to the user with a magic link`() {
    magicLinkEmailSender.send("an.email@company.com", "this-is-a-secret")

    verify(mockMailSender).send(
      check<SimpleMailMessage> {
        assertThat(it.to).isEqualTo(arrayOf("an.email@company.com"))
        assertThat(it.text).contains("some-url?secret=this-is-a-secret")
      }
    )
  }
}
