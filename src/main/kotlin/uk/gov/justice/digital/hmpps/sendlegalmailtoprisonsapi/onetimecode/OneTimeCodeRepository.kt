package uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.onetimecode

import org.springframework.data.annotation.Id
import org.springframework.data.redis.core.RedisHash
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

@Repository
interface OneTimeCodeRepository : CrudRepository<OneTimeCode, String>

@RedisHash(value = "oneTimeCodes")
data class OneTimeCode(
  @Id
  val sessionId: String,
  val code: String,
  val email: String,
)
