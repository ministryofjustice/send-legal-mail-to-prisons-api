package uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.onetimecode

import org.springframework.core.io.ClassPathResource
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.MimeMessageHelper
import org.springframework.mail.javamail.MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED
import org.springframework.stereotype.Service

@Service
class OneTimeCodeEmailSender(
  private val oneTimeCodeConfig: OneTimeCodeConfig,
  private val javaMailSender: JavaMailSender,
) {

  private companion object {
    fun textBodyTemplate(oneTimeCode: String, codeExpiry: String): String = """
      Your code for Send legal mail to prisons
      
      Your confirmation code is:
      $oneTimeCode

      You have $codeExpiry to use the code. You can only use it once.

      If you didn't request a code you can ignore this email.
      
      From Send legal mail to prisons team
      Ministry of Justice
    """.trimIndent()

    fun htmlBodyTemplate(oneTimeCode: String, codeExpiry: String): String = """
<html><head>
    <meta http-equiv="Content-Type" content="text/html charset=UTF-8">
    <title>Page Title</title>
    <style media="screen">
        body,html{
            margin:0;
            padding:0;
        }
    </style>
</head>
<body>
<table style="width:100%;background-color:#dee0e2;margin:0" cellspacing="0" cellpadding="0" border="0">
    <tbody>
    <tr>
        <td style="font-family:Helvetica,Arial,sans-serif;" align="center">
            <table style="background-color:#dee0e2;margin:30px auto;max-width:600px;overflow:hidden;" cellspacing="0" cellpadding="0">
                <tbody>
                <tr>
                    <td style="font-family:Helvetica,Arial,sans-serif">
                        <table style="background:#000000" width="100%" cellspacing="0" cellpadding="0">
                            <tbody>
                            <tr>
                                <td style="font-family:Helvetica,Arial,sans-serif;padding:4px 0;border-bottom:solid 10px #005ea5" width="30">

                                    <a href="https://www.gov.uk/" title="Go to the GOV.UK" style="font-size:24px;font-weight:bold;text-decoration:none;color:#ffffff" target="_blank" data-saferedirecturl="https://www.google.com/url?q=https://www.gov.uk/&amp;source=gmail&amp;ust=1648555909470000&amp;usg=AOvVaw2uN4hTGDbOHXGNCBJLkYyo">
                                        <img src="cid:crest.png" alt="" style="margin:10px 30px 10px 20px" class="CToWUd" width="30">
                                    </a>

                                </td>
                                <td style="font-family:Helvetica,Arial,sans-serif;padding:4px 0;border-bottom:solid 10px #005ea5">
                                    <a href="https://www.gov.uk/" title="Go to the GOV.UK" style="font-size:24px;font-weight:bold;text-decoration:none;color:#ffffff" target="_blank" data-saferedirecturl="https://www.google.com/url?q=https://www.gov.uk/&amp;source=gmail&amp;ust=1648555909470000&amp;usg=AOvVaw2uN4hTGDbOHXGNCBJLkYyo">GOV.UK</a>
                                </td>
                            </tr>
                            </tbody>
                        </table>
                    </td>
                </tr>
                <tr id="m_6852161939642515943contents">
                    <td style="font-family:Helvetica,Arial,sans-serif">
                        <table style="background:#ffffff" width="100%" cellspacing="0" cellpadding="0">
                            <tbody>
                            <tr>
                                <td style="font-family:Helvetica,Arial,sans-serif;padding:25px 20px 20px;line-height:1.11111">
                                    <h1 style="margin:0 0 30px;font-size:36px">Your code for Send legal mail to prisons</h1>
                                    <p style="font-family:Helvetica,Arial,sans-serif;margin:0 0 20px;font-size:19px">Your confirmation code is:</p>
                                    <p style="font-family:Helvetica,Arial,sans-serif;margin:0 0 20px;font-size:24px;font-weight:700;">$oneTimeCode</p>
                                    <p style="border-left: 10px solid #b1b4b6;display:block;font-family:Helvetica,Arial,sans-serif;margin:0 0 20px;font-size:19px;font-weight:700;padding-left:10px;"><br>You have $codeExpiry to use the code. You can use it only once.<br><br></p>
                                    <p style="font-family:Helvetica,Arial,sans-serif;margin:0 0 20px;font-size:19px;">If you didn't request a code you can ignore this email.</p>
                                    <p style="font-family:Helvetica,Arial,sans-serif;margin:0 0 20px;font-size:19px">Send legal mail to prisons team<br><br> Ministry of Justice</p>
                                </td>
                            </tr>
                            </tbody>
                        </table>
                    </td>
                </tr>
                <tr id="m_6852161939642515943footer" style="font-size:14px;color:#666666">
                    <td style="font-family:Helvetica,Arial,sans-serif">
                        <table width="100%" cellspacing="0" cellpadding="0">
                            <tbody>
                            <tr>
                                <td colspan="75%" style="font-family:Helvetica,Arial,sans-serif;padding:20px">
                                    This email has been transmitted via the Criminal Justice Secure eMail service.
                                    <br>
                                    <br>
                                    Anfonwyd y neges ebost hon drwy wasanaeth ebost Diogel Cyfiawnder Troseddo;
                                </td>
                                <td colspan="25%" style="font-family:Helvetica,Arial,sans-serif;padding:20px" align="right">
                                    <img src="cid:footer-image.png" tabindex="0" width="150" alt="">
                                </td>
                            </tr>
                            </tbody>
                        </table>
                    </td>
                </tr>
                </tbody>
            </table>
        </td>
    </tr>
    </tbody>
</table>
</body></html>
    """.trimIndent()
  }

  fun send(email: String, oneTmeCode: String) {
    val codeExpiry = "${oneTimeCodeConfig.codeExpiry.toMinutes()} minutes"

    javaMailSender.createMimeMessage()
      .also { mimeMessage ->
        MimeMessageHelper(mimeMessage, MULTIPART_MODE_MIXED_RELATED).apply {
          setTo(email)
          setSubject("Your code for Send legal mail to prisons")
          setText(textBodyTemplate(oneTmeCode, codeExpiry), htmlBodyTemplate(oneTmeCode, codeExpiry))
          addInline("crest.png", ClassPathResource("email/crest.png"))
          addInline("footer-image.png", ClassPathResource("email/footer-image.png"))
        }
      }
      .also { mimeMessage ->
        javaMailSender.send(mimeMessage)
      }
  }
}
