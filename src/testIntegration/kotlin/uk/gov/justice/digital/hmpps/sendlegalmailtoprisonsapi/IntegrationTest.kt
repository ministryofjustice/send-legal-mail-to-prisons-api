package uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi

import com.amazonaws.services.s3.AmazonS3
import com.microsoft.applicationinsights.TelemetryClient
import io.lettuce.core.ClientOptions
import org.awaitility.Awaitility
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.data.redis.LettuceClientConfigurationBuilderCustomizer
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.test.mock.mockito.SpyBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.http.HttpHeaders
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.reactive.server.WebTestClient
import redis.embedded.RedisServer
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.barcode.BarcodeConfig
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.barcode.BarcodeEventRepository
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.barcode.BarcodeEventService
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.barcode.BarcodeGeneratorService
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.barcode.BarcodeRecipientRepository
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.barcode.BarcodeReportingConfig
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.barcode.BarcodeRepository
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.barcode.BarcodeService
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.barcode.RandomCheckService
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.cjsm.CjsmDirectoryRepository
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.cjsm.CjsmService
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.cjsm.S3Config
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.contact.ContactRepository
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.magiclink.MagicLinkConfig
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.magiclink.MagicLinkSecretRepository
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.mocks.HmppsAuthExtension
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.mocks.PrisonRegisterExtension
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.mocks.PrisonerSearchExtension
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.onetimecode.OneTimeCodeAttemptsRepository
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.onetimecode.OneTimeCodeConfig
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.onetimecode.OneTimeCodeRepository
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.prisons.SupportedPrisonsRepository
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.prisons.SupportedPrisonsService
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.security.JwtService
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.security.SmokeTestConfig
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.testcontainers.LocalStackContainer
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.testcontainers.MailcatcherContainer
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.testcontainers.PostgresContainer
import java.util.concurrent.TimeUnit.SECONDS
import javax.annotation.PostConstruct
import javax.annotation.PreDestroy

@SpringBootTest(webEnvironment = RANDOM_PORT)
@Import(IntegrationTest.RedisConfig::class)
@ExtendWith(HmppsAuthExtension::class, PrisonerSearchExtension::class, PrisonRegisterExtension::class)
@ActiveProfiles("test")
abstract class IntegrationTest {

  @Suppress("SpringJavaInjectionPointsAutowiringInspection")
  @Autowired
  lateinit var webTestClient: WebTestClient

  @Autowired
  protected lateinit var jwtAuthHelper: JwtAuthHelper

  @SpyBean
  protected lateinit var barcodeRepository: BarcodeRepository

  @SpyBean
  protected lateinit var barcodeEventRepository: BarcodeEventRepository

  @SpyBean
  protected lateinit var barcodeGeneratorService: BarcodeGeneratorService

  @Autowired
  protected lateinit var magicLinkSecretRepository: MagicLinkSecretRepository

  @Autowired
  protected lateinit var magicLinkConfig: MagicLinkConfig

  @Autowired
  protected lateinit var oneTimeCodeRepository: OneTimeCodeRepository

  @Autowired
  protected lateinit var oneTimeCodeAttemptsRepository: OneTimeCodeAttemptsRepository

  @Autowired
  protected lateinit var oneTimeCodeConfig: OneTimeCodeConfig

  @Autowired
  protected lateinit var jwtService: JwtService

  @SpyBean
  protected lateinit var barcodeConfig: BarcodeConfig

  @SpyBean
  protected lateinit var randomCheckService: RandomCheckService

  @Autowired
  protected lateinit var cjsmDirectoryRepository: CjsmDirectoryRepository

  @SpyBean
  protected lateinit var amazonS3: AmazonS3

  @Autowired
  protected lateinit var s3Config: S3Config

  @Autowired
  protected lateinit var cjsmService: CjsmService

  @Autowired
  protected lateinit var contactRepository: ContactRepository

  @SpyBean
  protected lateinit var barcodeRecipientRepository: BarcodeRecipientRepository

  @Autowired
  protected lateinit var barcodeService: BarcodeService

  @Autowired
  protected lateinit var barcodeEventService: BarcodeEventService

  @SpyBean
  lateinit var telemetryClient: TelemetryClient

  @SpyBean
  protected lateinit var barcodeReportingConfig: BarcodeReportingConfig

  @SpyBean
  protected lateinit var smokeTestConfig: SmokeTestConfig

  @SpyBean
  protected lateinit var supportedPrisonsService: SupportedPrisonsService

  @Autowired
  protected lateinit var supportedPrisonsRepository: SupportedPrisonsRepository

  @BeforeEach
  fun `turn off random checks`() {
    doReturn(false).whenever(randomCheckService).requiresRandomCheck()
  }

  @AfterEach
  fun `clear database`() {
    barcodeRecipientRepository.deleteAll()
    barcodeEventRepository.deleteAll()
    barcodeRepository.deleteAll()
    magicLinkSecretRepository.deleteAll()
    cjsmDirectoryRepository.deleteAll()
    contactRepository.deleteAll()
    oneTimeCodeRepository.deleteAll()
    oneTimeCodeAttemptsRepository.deleteAll()
    supportedPrisonsRepository.deleteAll()
  }

  @AfterEach
  fun `reset mocks and spies`() {
    reset(barcodeRecipientRepository, amazonS3, randomCheckService, barcodeConfig, barcodeGeneratorService, barcodeEventRepository, barcodeRepository, smokeTestConfig, supportedPrisonsService)
  }

  internal fun setAuthorisation(
    user: String? = "send-legal-mail-client",
    roles: List<String> = listOf("ROLE_SLM_EMAIL_LINK"),
    scopes: List<String> = listOf(),
  ): (HttpHeaders) -> Unit = jwtAuthHelper.setAuthorisation(user, roles, scopes)

  internal fun setSlmClientIp(): (HttpHeaders) -> Unit = { it.set("x-slm-client-ip", "127.0.0.1") }

  internal fun setCreateBarcodeAuthorisation(email: String = "some.user@company.com.cjsm.net"): (HttpHeaders) -> Unit =
    jwtAuthHelper.setCreateBarcodeAuthorisation(email)

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
            .build(),
        )
      }
  }

  companion object {
    private val pgContainer = PostgresContainer.instance
    private val mailcatcherContainer = MailcatcherContainer.instance
    private val localStackContainer = LocalStackContainer.instance

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
      mailcatcherContainer?.run {
        registry.add("spring.mail.port") { getMappedPort(1025) }
        registry.add("mailcatcher.api.port") { getMappedPort(1080) }
      }
      localStackContainer?.run {
        registry.add("app.s3.localstack-url") {
          localStackContainer.getEndpointOverride(org.testcontainers.containers.localstack.LocalStackContainer.Service.S3)
        }
        registry.add("app.s3.region") { localStackContainer.region }
      }
    }

    @JvmStatic
    @BeforeAll
    fun `set defaults`() {
      Awaitility.setDefaultTimeout(5, SECONDS)
    }
  }
}
