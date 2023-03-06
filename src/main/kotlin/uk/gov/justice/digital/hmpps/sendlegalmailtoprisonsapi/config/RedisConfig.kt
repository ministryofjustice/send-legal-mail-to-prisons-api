package uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.config

import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.core.RedisKeyValueAdapter
import org.springframework.data.redis.core.convert.KeyspaceConfiguration
import org.springframework.data.redis.repository.configuration.EnableRedisRepositories
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.magiclink.MagicLinkConfig
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.magiclink.MagicLinkSecret
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.onetimecode.OneTimeCode
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.onetimecode.OneTimeCodeConfig

// we want keyspace notifications, but have to empty the config parameter (default Ex) since elasticache doesn't support
// changing the config.  If we move off elasticache then need to remove the config parameter and let it use the default.
@Configuration
@EnableRedisRepositories(
  enableKeyspaceEvents = RedisKeyValueAdapter.EnableKeyspaceEvents.ON_STARTUP,
  keyspaceNotificationsConfigParameter = "\${standalone-redis.keyspace-notifications:}",
  keyspaceConfiguration = OneTimeCodeKeyspaceConfiguration::class,
)
class RedisConfig

// TODO Remove this when removing the magic link code
class SecretKeyspaceConfiguration(private val magicLinkConfig: MagicLinkConfig) : KeyspaceConfiguration() {
  override fun hasSettingsFor(type: Class<*>?): Boolean {
    return type?.equals(MagicLinkSecret::class.java) ?: false
  }

  override fun getKeyspaceSettings(type: Class<*>?): KeyspaceSettings {
    val keyspaceSettings = KeyspaceSettings(MagicLinkSecret::class.java, "MagicLinkSecret")
    keyspaceSettings.timeToLive = magicLinkConfig.secretExpiry.toSeconds()
    return keyspaceSettings
  }
}
class OneTimeCodeKeyspaceConfiguration(private val oneTimeCodeConfig: OneTimeCodeConfig) : KeyspaceConfiguration() {
  override fun hasSettingsFor(type: Class<*>?): Boolean {
    return true
  }

  override fun getKeyspaceSettings(type: Class<*>?): KeyspaceSettings {
    val keyspaceSettings = KeyspaceSettings(OneTimeCode::class.java, "OneTimeCode")
    keyspaceSettings.timeToLive = oneTimeCodeConfig.codeExpiry.toSeconds()
    return keyspaceSettings
  }
}
