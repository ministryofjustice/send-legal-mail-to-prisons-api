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
      Your code for Send legal mail to prisons
      
      You need a code to start using Send legal mail to prisons.

      Your confirmation code is:
      %s

      You have %s to use the code. You can only use it once.

      If you didn't request a code you can ignore this email.
      
      From Send legal mail to prisons team
      Ministry of Justice
    """.trimIndent()

    val HTML_BODY_TEMPLATE: String = """
      <html>
      <head></head>
      <body>
        <p>
          <strong>Your code for Send legal mail to prisons</strong>
        </p>
        <p>
          You need a code to start using Send legal mail to prisons.
        </p>
        <p>
          Your confirmation code is:<br/>
          <strong>%s</strong>
        </p>

        <p>
          <strong>You have %s to use the code. You can only use it once.</strong>
        </p>

        <p>If you didn't request a code you can ignore this email.</p>

        <p>
          From Send legal mail to prisons team<br/>
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
