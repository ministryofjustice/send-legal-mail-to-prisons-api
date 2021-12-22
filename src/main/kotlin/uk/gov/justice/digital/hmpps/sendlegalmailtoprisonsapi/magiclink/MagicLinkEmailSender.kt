package uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.magiclink

import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.MimeMessageHelper
import org.springframework.stereotype.Service

@Service
class MagicLinkEmailSender(
  private val magicLinkConfig: MagicLinkConfig,
  private val javaMailSender: JavaMailSender,
) {

  private companion object {
    val TEXT_BODY_TEMPLATE: String = """
      You requested a link to log in to Send legal mail to prisons.

      Click on the link or paste it into your browser:
      %s

      You have %s to use the link.

      If you didn't request a link you can ignore this email.
      From HMPPS Digital
    """.trimIndent()

    val HTML_BODY_TEMPLATE: String = """
      <html>
      <head></head>
      <body>
        <p>
          You requested a link to log in to Send legal mail to prisons.
        </p>

        <p>
          Click on the link or paste it into your browser:<br/>
          <a href="%s">%s</a>
        </p>

        <p>You have %s to use the link.</p>

        <p>
          If you didn't request a link you can ignore this email.<br/>
          From HMPPS Digital
        </p>
      </body>
      </html>
    """.trimIndent()
  }

  fun send(email: String, secret: String) {
    val magicLink = "${magicLinkConfig.url}?secret=$secret"
    val magicLinkExpiry = "${magicLinkConfig.secretExpiry.toMinutes()} minutes"

    javaMailSender.createMimeMessage()
      .also { mimeMessage ->
        MimeMessageHelper(mimeMessage, true).apply {
          setTo(email)
          setSubject("Send Legal Mail Sign in")
          setText(textBody(magicLink, magicLinkExpiry), htmlBody(magicLink, magicLinkExpiry))
        }
      }
      .also { mimeMessage ->
        javaMailSender.send(mimeMessage)
      }
  }

  private fun textBody(magicLink: String, magicLinkExpiry: String) = String.format(TEXT_BODY_TEMPLATE, magicLink, magicLinkExpiry)

  private fun htmlBody(magicLink: String, magicLinkExpiry: String) = String.format(HTML_BODY_TEMPLATE, magicLink, magicLink, magicLinkExpiry)
}
