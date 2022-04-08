package uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.onetimecode

import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.MimeMessageHelper
import org.springframework.stereotype.Service

@Service
class OneTimeCodeEmailSender(
  private val oneTimeCodeConfig: OneTimeCodeConfig,
  private val javaMailSender: JavaMailSender,
) {

  private companion object {
    val TEXT_BODY_TEMPLATE: String = """
      You requested a code to sign in to Send legal mail to prisons.

      Your code is:
      %s

      You have %s to use the code.

      If you didn't request a code you can ignore this email.
      
      Send legal mail to prisons team
      Ministry of Justice
    """.trimIndent()

    val HTML_BODY_TEMPLATE: String = """
      <html>
      <head></head>
      <body>
        <p>
          You requested a code to sign in to Send legal mail to prisons.
        </p>

        <p>
          Your code is:<br/>
          %s
        </p>

        <p>You have %s to use the code.</p>

        <p>If you didn't request a code you can ignore this email.</p>

        <p>
          <strong>Send legal mail to prisons team</strong><br/>
          Ministry of Justice
        </p>
      </body>
      </html>
    """.trimIndent()
  }

  fun send(email: String, oneTmeCode: String) {
    val codeExpiry = "${oneTimeCodeConfig.codeExpiry.toMinutes()} minutes"

    javaMailSender.createMimeMessage()
      .also { mimeMessage ->
        MimeMessageHelper(mimeMessage, true).apply {
          setTo(email)
          setSubject("Sign in to Send legal mail to prisons")
          setText(textBody(oneTmeCode, codeExpiry), htmlBody(oneTmeCode, codeExpiry))
        }
      }
      .also { mimeMessage ->
        javaMailSender.send(mimeMessage)
      }
  }

  private fun textBody(code: String, codeExpiry: String) = String.format(TEXT_BODY_TEMPLATE, code, codeExpiry)

  private fun htmlBody(code: String, codeExpiry: String) = String.format(HTML_BODY_TEMPLATE, code, codeExpiry)
}
