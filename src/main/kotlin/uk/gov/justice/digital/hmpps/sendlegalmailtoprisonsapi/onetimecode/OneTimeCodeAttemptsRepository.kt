package uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.onetimecode

import org.springframework.data.annotation.Id
import org.springframework.data.redis.core.RedisHash
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

@Repository
interface OneTimeCodeAttemptsRepository : CrudRepository<OneTimeCodeAttempts, String>

@RedisHash(value = "oneTimeCodeAttempts")
data class OneTimeCodeAttempts(
  @Id
  val sessionId: String,
  var attempts: Set<String>?,
)
