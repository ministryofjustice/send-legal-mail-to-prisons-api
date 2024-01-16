package uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.onetimecode

import jakarta.mail.Session
import jakarta.mail.internet.MimeMessage
import jakarta.mail.internet.MimeMultipart
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.check
import org.mockito.kotlin.given
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.springframework.mail.javamail.JavaMailSender
import java.time.Duration
import java.time.temporal.ChronoUnit

class OneTimeCodeEmailSenderTest {
  private val mockMailSender: JavaMailSender = mock()
  private val magicLinkEmailSender = OneTimeCodeEmailSender(
    OneTimeCodeConfig(Duration.of(10, ChronoUnit.MINUTES), 4, listOf('A', 'B', 'C', 'D'), 6),
    mockMailSender,
  )

  @Test
  fun `The mail should be sent to the user with a one time code`() {
    val session: Session? = null
    val mimeMessage = MimeMessage(session)
    given(mockMailSender.createMimeMessage()).willReturn(mimeMessage)

    magicLinkEmailSender.send("an.email@company.com", "ABCD")

    verify(mockMailSender).send(
      check<MimeMessage> {
        assertThat(it.subject).isEqualTo("Your code for Send legal mail to prisons")
        assertThat(it.allRecipients.map { recipient -> recipient.toString() })
          .isEqualTo(listOf("an.email@company.com"))

        val bodyPartContents = (it.content as MimeMultipart).getBodyPartContents()
        assertThat(bodyPartContents["text/plain"]).contains("10 minutes")
        assertThat(bodyPartContents["text/plain"]).contains("ABCD")
        assertThat(bodyPartContents["text/html"]).contains("10 minutes")
        assertThat(bodyPartContents["text/html"]).contains("ABCD")
      },
    )
  }

  private fun MimeMultipart.getBodyPartContents(): Map<String, String> {
    val bodyPartContents: MutableMap<String, String> = mutableMapOf()
    for (idx in 0 until this.count) {
      val bodyPart = this.getBodyPart(idx)
      if (bodyPart.content is MimeMultipart) {
        bodyPartContents.putAll((bodyPart.content as MimeMultipart).getBodyPartContents())
      } else {
        bodyPartContents[bodyPart.dataHandler.contentType.substringBefore(";")] = bodyPart.dataHandler.content.toString()
      }
    }
    return bodyPartContents.toMap()
  }
}
