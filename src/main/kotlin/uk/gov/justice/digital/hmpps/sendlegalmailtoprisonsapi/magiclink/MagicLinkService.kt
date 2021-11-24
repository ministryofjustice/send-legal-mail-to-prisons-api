package uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.magiclink

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.security.JwtService
import java.util.Optional
import java.util.UUID
import javax.persistence.EntityNotFoundException

@Service
class MagicLinkService(
  private val magicLinkEmailSender: MagicLinkEmailSender,
  private val magicLinkSecretRepository: MagicLinkSecretRepository,
  private val jwtService: JwtService,
) {

  fun createAndSendMagicLink(email: String) {
    MagicLinkSecret(UUID.randomUUID().toString(), email)
      .also { secret -> magicLinkSecretRepository.save(secret) }
      .also { secret -> magicLinkEmailSender.send(secret.email, secret.secretValue) }
  }

  fun verifyMagicLinkSecret(secret: String): String =
    magicLinkSecretRepository.findById(secret).toNullable()
      ?.also { magicLinkSecretRepository.deleteById(secret) }
      ?.let { savedSecret -> jwtService.generateToken(savedSecret.email) }
      ?: throw EntityNotFoundException("Not found")
}

fun <T> Optional<T>.toNullable(): T? = orElse(null)
