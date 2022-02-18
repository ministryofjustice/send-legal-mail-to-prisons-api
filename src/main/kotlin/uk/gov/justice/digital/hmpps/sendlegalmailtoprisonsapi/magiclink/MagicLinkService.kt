package uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.magiclink

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.cjsm.CjsmService
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.config.ResourceNotFoundException
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.security.JwtService
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.toNullable
import java.util.UUID

@Service
class MagicLinkService(
  private val magicLinkEmailSender: MagicLinkEmailSender,
  private val magicLinkSecretRepository: MagicLinkSecretRepository,
  private val jwtService: JwtService,
  private val cjsmService: CjsmService,
) {

  fun createAndSendMagicLink(email: String) {
    MagicLinkSecret(UUID.randomUUID().toString(), email)
      .also { secret -> magicLinkSecretRepository.save(secret) }
      .also { secret -> magicLinkEmailSender.send(secret.email, secret.secretValue) }
  }

  fun verifyMagicLinkSecret(secret: String): String =
    magicLinkSecretRepository.findById(secret).toNullable()
      ?.also { magicLinkSecretRepository.deleteById(secret) }
      ?.let { savedSecret -> jwtService.generateToken(savedSecret.email, findOrganisation(savedSecret.email)) }
      ?: throw ResourceNotFoundException("Magic Link not found")

  private fun findOrganisation(userId: String): String? =
    cjsmService.findOrganisation(userId)
      ?.takeIf { it.isNotBlank() }
}
