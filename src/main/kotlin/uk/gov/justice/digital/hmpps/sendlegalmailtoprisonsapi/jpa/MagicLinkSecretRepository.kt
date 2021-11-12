package uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.jpa

import org.springframework.data.annotation.Id
import org.springframework.data.redis.core.RedisHash
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

@Repository
interface MagicLinkSecretRepository : CrudRepository<MagicLinkSecret, String>

@RedisHash(value = "magiclinkSecrets")
data class MagicLinkSecret(
  @Id
  val secretValue: String,
  val email: String,
)
