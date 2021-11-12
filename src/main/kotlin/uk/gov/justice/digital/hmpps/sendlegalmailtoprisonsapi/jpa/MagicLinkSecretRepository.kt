package uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.jpa

import org.springframework.data.annotation.Id
import org.springframework.data.redis.core.RedisHash
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

@Repository
interface MagicLinkSecretRepository : CrudRepository<MagicLinkSecret, String>

@RedisHash(value = "sessionSecrets")
data class MagicLinkSecret(
  @Id
  val email: String,
  val secretValue: String,
)
