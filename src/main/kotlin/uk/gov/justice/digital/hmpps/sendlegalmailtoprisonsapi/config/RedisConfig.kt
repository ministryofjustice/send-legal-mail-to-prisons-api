package uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.config

import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.core.RedisKeyValueAdapter
import org.springframework.data.redis.core.convert.KeyspaceConfiguration
import org.springframework.data.redis.repository.configuration.EnableRedisRepositories
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.magiclink.MagicLinkSecret

// we want keyspace notifications, but have to empty the config parameter (default Ex) since elasticache doesn't support
// changing the config.  If we move off elasticache then need to remove the config parameter and let it use the default.
@Configuration
@EnableRedisRepositories(
  enableKeyspaceEvents = RedisKeyValueAdapter.EnableKeyspaceEvents.ON_STARTUP,
  keyspaceNotificationsConfigParameter = "\${standalone-redis.keyspace-notifications:}",
  keyspaceConfiguration = SecretKeyspaceConfiguration::class
)
class RedisConfig

class SecretKeyspaceConfiguration(private val magicLinkConfig: MagicLinkConfig) : KeyspaceConfiguration() {
  override fun hasSettingsFor(type: Class<*>?): Boolean {
    return true
  }

  override fun getKeyspaceSettings(type: Class<*>?): KeyspaceSettings {
    val keyspaceSettings = KeyspaceSettings(MagicLinkSecret::class.java, "MyHashlog")
    keyspaceSettings.timeToLive = magicLinkConfig.secretExpiry.toSeconds()
    return keyspaceSettings
  }
}
