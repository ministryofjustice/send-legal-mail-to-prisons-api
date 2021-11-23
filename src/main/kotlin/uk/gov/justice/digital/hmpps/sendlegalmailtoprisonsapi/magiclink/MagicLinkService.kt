package uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.magiclink

import org.springframework.stereotype.Service
import java.util.UUID

@Service
class MagicLinkService(
  private val magicLinkEmailSender: MagicLinkEmailSender,
  private val magicLinkSecretRepository: MagicLinkSecretRepository,
) {

  fun createAndSendMagicLink(email: String) {
    MagicLinkSecret(UUID.randomUUID().toString(), email)
      .also { secret -> magicLinkSecretRepository.save(secret) }
      .also { secret -> magicLinkEmailSender.send(secret.email, secret.secretValue) }
  }
}
