package uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.security

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.config.JwtConfig
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit

class JwtServiceTest {

  private val privateKey = "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"
  private val publicKey = "MIIBojANBgkqhkiG9w0BAQEFAAOCAY8AMIIBigKCAYEAueyd69Z1oIVQJVAW34x+oXpeF/uLsmAh6qmpPzM1x0JuIoAO+c9LgU5fenFgqGzIBNb3XScldnVt5Hpu1Tzktjfq24FOa02ZiNHdkirSFZ4ORsDQlAMKst6c1cBF2I9gSyJNJ4zTgmfO6wFbChvy4ZFr3FbhkIhSO/LWEoFoQff3gqPAdwCSwHo7gZOgmoZuCwe5gRbSG3c3GQolGwhzxXaA5WtUbaJT8y8bQ2xA0jee0Dq/0sX/indHsHaY7JZAoUJ5diSlVmg68d1OHi/Vr6Y034+Uhlrx4O99cL/aZFiNumC0VXH90mHcUCBNm7uhrew4bRvxlvKXFWOPHpag8SBvSeB8z7D16D7aYcF6Z/Ul1BDU8lkElEY9Zv8mo3WR6E7rmT7WDUngAX2g3Saj6VSnGXHZuCFrT43OoBS5BDAHY6QHwxAk8EGrbM9aHCbXCvzCGgK3M5QMZC/Cj4G7x1916Df0KPgbPSuTB6w8cCjOVeODqp8aRuUPsOHmaOoXAgMBAAE="
  private val defaultExpiry = Duration.of(10, ChronoUnit.MINUTES)

  private fun jwtService(
    expiry: Duration = defaultExpiry,
    clock: Clock = Clock.fixed(Instant.now(), ZoneId.of("Europe/London"))
  ) = JwtService(JwtConfig(privateKey, publicKey, expiry), clock)

  @Test
  fun `can generate and validate a JWT`() {
    val jwtService = jwtService()
    val jwt = jwtService.generateToken("some.email@company.com")

    assertThat(jwtService.validateToken(jwt)).isTrue
  }

  @Test
  fun `can retrieve the subject from a generated JWT`() {
    val jwtService = jwtService()
    val jwt = jwtService.generateToken("some.email@company.com")

    assertThat(jwtService.subject(jwt)).isEqualTo("some.email@company.com")
  }

  @Test
  fun `the generated JWT should have the create barcode role`() {
    val jwtService = jwtService()
    val jwt = jwtService.generateToken("some.email@company.com")

    assertThat(jwtService.authorities(jwt)).containsExactly("ROLE_SLM_CREATE_BARCODE")
  }

  @Test
  fun `the generated JWT should expire at midnight on the day of expiry`() {
    val jwtService = jwtService(
      expiry = Duration.of(1, ChronoUnit.DAYS),
      clock = Clock.fixed(Instant.parse("2021-11-26T12:18:05Z"), ZoneId.of("Europe/London"))
    )
    val jwt = jwtService.generateToken("some.email@company.com")

    val expiresAt = jwtService.expiresAt(jwt)
    assertThat(expiresAt).isEqualTo(Instant.parse("2021-11-28T00:00:00Z"))
  }

  @Test
  fun `an expired JWT should be invalid`() {
    val jwtService = jwtService(Duration.of(-1, ChronoUnit.DAYS))
    val jwt = jwtService.generateToken("some.email@company.com")

    assertThat(jwtService.validateToken(jwt)).isFalse
  }

  @Test
  fun `a JWT signed elsewhere should be invalid`() {
    val jwtService = jwtService()
    val alienJwt =
      "eyJhbGciOiJSUzI1NiJ9.eyJqdGkiOiJkZTE3NGM0Yi0xY2M2LTQxYWYtYTczYi01ZTE2YmI5YzE1ZWIiLCJzdWIiOiJtaWtlLmhhbG1hQGRpZ2l0YWwuanVzdGljZS5nb3YudWsiLCJleHAiOjQ3ODc1Njk3MjF9.WTqNajHRgZCbNe0g20lK5a7s_5-VeWD-FViu6gTgQaEsavimH_wEz1wZ4sj5osCDkCaLIgjYxGFt_p2IAsr7x0pI5b3CenN4_EMrz2pVVxAXOEEI8Q8QVfTy-iBGyO9W95rFGtmxbdsmYpr7LIr6DxJDUCCrCoeH8f4Dl-4QfKLUn-x_9_Bfum1rtAJ38B5pwiwhlzxeHD58C5XIc7swURGpCA97gtog7kEbyrCDF5AkIM4oYC1ViTMfDypnIJaDAU2ggxkaV5EkiIOB386POjUXkePQDnPajX3C-ugbJlKUPHp9z0CL_ngw5iK3wf9mEy2mWi9VHbUnyqVzfhrbIJK2PKQ0Fb8ZJIZhlB_rD68bgpaKskJwGy3lCMqDV5hiK5rUMsw_6n0asdYIhOvrEkXHrwmR4eRfobkLmtXGGRBswWuMhVXbYxBfZPU4PSkReTnbGRxSub-_UmMIvI_CXXaMdyRv0ixG4R3R7HfgLyZiTffN0p8nKmzKDXWWmPVJ"

    assertThat(jwtService.validateToken(alienJwt)).isFalse
  }
}
