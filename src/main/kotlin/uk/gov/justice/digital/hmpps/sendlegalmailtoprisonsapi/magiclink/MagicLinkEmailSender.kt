package uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.magiclink

import org.springframework.mail.SimpleMailMessage
import org.springframework.mail.javamail.JavaMailSender
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.config.MagicLinkConfig

class MagicLinkEmailSender(
  private val magicLinkConfig: MagicLinkConfig,
  private val javaMailSender: JavaMailSender,
) {

  fun send(email: String, secret: String) {
    SimpleMailMessage().apply {
      setTo(email)
      subject = "Send Legal Mail Sign in"
      text = "${magicLinkConfig.url}?secret=$secret"
    }
      .also { javaMailSender.send(it) }
  }
}
