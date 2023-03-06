package uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.magiclink

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.check
import org.mockito.kotlin.given
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.springframework.mail.javamail.JavaMailSender
import java.time.Duration
import java.time.temporal.ChronoUnit
import javax.mail.Session
import javax.mail.internet.MimeMessage
import javax.mail.internet.MimeMultipart

class MagicLinkEmailSenderTest {

  private val mockMailSender: JavaMailSender = mock()
  private val magicLinkEmailSender = MagicLinkEmailSender(MagicLinkConfig(Duration.of(10, ChronoUnit.MINUTES), "some-url"), mockMailSender)

  @Test
  fun `The mail should be sent to the user with a magic link`() {
    val session: Session? = null
    val mimeMessage = MimeMessage(session)
    given(mockMailSender.createMimeMessage()).willReturn(mimeMessage)

    magicLinkEmailSender.send("an.email@company.com", "this-is-a-secret")

    verify(mockMailSender).send(
      check<MimeMessage> {
        assertThat(it.subject).isEqualTo("Sign in to Send legal mail to prisons")
        assertThat(it.allRecipients.map { recipient -> recipient.toString() }).isEqualTo(listOf("an.email@company.com"))

        val bodyPartContents = (it.content as MimeMultipart).getBodyPartContents()
        assertThat(bodyPartContents["text/plain"]).contains("10 minutes")
        assertThat(bodyPartContents["text/plain"]).contains("some-url?secret=this-is-a-secret")
        assertThat(bodyPartContents["text/html"]).contains("10 minutes")
        assertThat(bodyPartContents["text/html"]).contains("<a href=\"some-url?secret=this-is-a-secret\"")
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
