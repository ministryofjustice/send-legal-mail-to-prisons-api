package uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.config

import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.core.RedisKeyValueAdapter
import org.springframework.data.redis.repository.configuration.EnableRedisRepositories

// This overrides the config to replace EnableKeyspaceEvents from ON_STARTUP to ON_DEMAND
@Configuration
@EnableRedisRepositories(
  enableKeyspaceEvents = RedisKeyValueAdapter.EnableKeyspaceEvents.ON_DEMAND,
  keyspaceNotificationsConfigParameter = "\${standalone-redis.keyspace-notifications:}",
)
class RedisConfig
