package uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.config

import org.springframework.context.annotation.Configuration

// we want keyspace notifications, but have to empty the config parameter (default Ex) since elasticache doesn't support
// changing the config.  If we move off elasticache then need to remove the config parameter and let it use the default.
// TODO - remove this altogether
@Configuration
class RedisConfig
