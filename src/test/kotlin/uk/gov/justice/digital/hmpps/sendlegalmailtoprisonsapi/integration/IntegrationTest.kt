package uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.integration

import io.lettuce.core.ClientOptions
import org.junit.jupiter.api.AfterEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.data.redis.LettuceClientConfigurationBuilderCustomizer
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.reactive.server.WebTestClient
import redis.embedded.RedisServer
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.integration.testcontainers.PostgresContainer
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.jpa.BarcodeRepository
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.jpa.MagicLinkSecretRepository
import javax.annotation.PostConstruct
import javax.annotation.PreDestroy

@SpringBootTest(webEnvironment = RANDOM_PORT)
@Import(IntegrationTest.RedisConfig::class)
@ActiveProfiles("test")
abstract class IntegrationTest {

  @Suppress("SpringJavaInjectionPointsAutowiringInspection")
  @Autowired
  lateinit var webTestClient: WebTestClient

  @Autowired
  protected lateinit var barcodeRepository: BarcodeRepository

  @Autowired
  protected lateinit var magicLinkSecretRepository: MagicLinkSecretRepository

  @AfterEach
  fun `clear database`() {
    barcodeRepository.deleteAll()
  }

  @TestConfiguration
  class RedisConfig {
    private val redisServer: RedisServer = RedisServer(6380)

    @PostConstruct
    fun postConstruct() {
      redisServer.start()
    }

    @PreDestroy
    fun preDestroy() {
      redisServer.stop()
    }

    @Bean
    fun lettuceClientConfigurationBuilderCustomizer(): LettuceClientConfigurationBuilderCustomizer =
      LettuceClientConfigurationBuilderCustomizer {
        it.clientOptions(
          ClientOptions.builder()
            .disconnectedBehavior(ClientOptions.DisconnectedBehavior.REJECT_COMMANDS)
            .build()
        )
      }
  }

  companion object {
    private val pgContainer = PostgresContainer.instance

    @JvmStatic
    @DynamicPropertySource
    fun properties(registry: DynamicPropertyRegistry) {
      pgContainer?.run {
        registry.add("spring.datasource.url", pgContainer::getJdbcUrl)
        registry.add("spring.datasource.username", pgContainer::getUsername)
        registry.add("spring.datasource.password", pgContainer::getPassword)
        registry.add("spring.datasource.placeholders.database_update_password", pgContainer::getPassword)
        registry.add("spring.datasource.placeholders.database_read_only_password", pgContainer::getPassword)
        registry.add("spring.flyway.url", pgContainer::getJdbcUrl)
        registry.add("spring.flyway.user", pgContainer::getUsername)
        registry.add("spring.flyway.password", pgContainer::getPassword)
      }
    }
  }
}
