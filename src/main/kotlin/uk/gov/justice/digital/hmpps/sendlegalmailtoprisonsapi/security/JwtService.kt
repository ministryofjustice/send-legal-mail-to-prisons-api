package uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.security

import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.SignedJWT
import io.jsonwebtoken.Jwts
import mu.KotlinLogging
import org.springframework.stereotype.Service
import java.security.KeyFactory
import java.security.PrivateKey
import java.security.PublicKey
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import java.text.ParseException
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.Base64
import java.util.Date
import java.util.UUID

private val log = KotlinLogging.logger {}

@Service
class JwtService(jwtConfig: JwtConfig, private val smokeTestConfig: SmokeTestConfig, private val clock: Clock) {

  private val privateKey: PrivateKey = readPrivateKey(jwtConfig.privateKey)
  private val publicKey: PublicKey = readPublicKey(jwtConfig.publicKey)
  private val expiry: Duration = jwtConfig.expiry

  private fun readPrivateKey(privateKeyString: String): PrivateKey = PKCS8EncodedKeySpec(Base64.getDecoder().decode(privateKeyString))
    .let { KeyFactory.getInstance("RSA").generatePrivate(it) }

  private fun readPublicKey(publicKeyString: String): PublicKey = X509EncodedKeySpec(Base64.getDecoder().decode(publicKeyString))
    .let { KeyFactory.getInstance("RSA").generatePublic(it) }

  fun generateToken(email: String, organisation: String?): String = Jwts.builder()
    .id(UUID.randomUUID().toString())
    .subject(email)
    .expiration(Date.from(calculateExpiryAtMidnight(expiry)))
    .claims(
      mapOf(
        "authorities" to listOf("ROLE_SLM_CREATE_BARCODE"),
        "client_id" to "send-legal-mail",
        "user_name" to email,
        "organisation" to organisation,
      ),
    )
    .signWith(privateKey, Jwts.SIG.RS256)
    .compact()

  private fun calculateExpiryAtMidnight(expiry: Duration) = Instant.now(clock)
    .plus(expiry.toMillis(), ChronoUnit.MILLIS)
    .plus(1, ChronoUnit.DAYS)
    .truncatedTo(ChronoUnit.DAYS)

  fun validateToken(jwt: String): Boolean = runCatching {
    Jwts.parser().verifyWith(publicKey).build().parseSignedClaims(jwt)
  }.onFailure {
    log.warn("Found an invalid JWT: $jwt", it)
  }.isSuccess

  fun subject(jwt: String): String = Jwts.parser().verifyWith(publicKey).build().parseSignedClaims(jwt).payload.subject

  fun organisation(jwt: String): String? = Jwts.parser().verifyWith(publicKey).build().parseSignedClaims(jwt).payload["organisation"] as? String

  @Suppress("UNCHECKED_CAST")
  fun authorities(jwt: String): List<String>? = Jwts.parser().verifyWith(publicKey).build().parseSignedClaims(jwt).payload["authorities"] as? List<String>

  fun clientId(jwt: String): String? = Jwts.parser().verifyWith(publicKey).build().parseSignedClaims(jwt).payload["client_id"] as? String

  fun expiresAt(jwt: String): Instant = Jwts.parser().verifyWith(publicKey).build().parseSignedClaims(jwt).payload.expiration.toInstant()

  fun isSmokeTestUserToken(authToken: String?) = authToken?.isNotBlank()
    ?.and(getUser(authToken)?.lowercase() == smokeTestConfig.msjSecret?.lowercase())
    ?: false

  fun isNomisUserToken(authToken: String?) = authToken?.isNotBlank()
    ?.and(getUser(authToken).isNullOrBlank().not())
    ?: false

  fun getUser(authToken: String): String? = getClaimsFromJWT(authToken).getClaim("user_name") as String?

  @Throws(ParseException::class)
  private fun getClaimsFromJWT(token: String): JWTClaimsSet = SignedJWT.parse(token.replace("Bearer ", "")).jwtClaimsSet
}
