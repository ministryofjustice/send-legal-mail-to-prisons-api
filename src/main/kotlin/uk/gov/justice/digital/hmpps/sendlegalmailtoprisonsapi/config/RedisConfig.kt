package uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.config

import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.core.RedisKeyValueAdapter
import org.springframework.data.redis.repository.configuration.EnableRedisRepositories

// we want keyspace notifications, but have to empty the config parameter (default Ex) since elasticache doesn't support
// changing the config.  If we move off elasticache then need to remove the config parameter and let it use the default.
@Configuration
@EnableRedisRepositories(
  enableKeyspaceEvents = RedisKeyValueAdapter.EnableKeyspaceEvents.ON_STARTUP,
  keyspaceNotificationsConfigParameter = "\${standalone-redis.keyspace-notifications:}",
)
class RedisConfig
