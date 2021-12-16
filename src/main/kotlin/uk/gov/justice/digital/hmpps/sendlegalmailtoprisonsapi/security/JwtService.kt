package uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.security

import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.SignedJWT
import io.jsonwebtoken.ExpiredJwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
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
class JwtService(jwtConfig: JwtConfig, private val clock: Clock) {

  private val privateKey: PrivateKey = readPrivateKey(jwtConfig.privateKey)
  private val publicKey: PublicKey = readPublicKey(jwtConfig.publicKey)
  private val expiry: Duration = jwtConfig.expiry

  private fun readPrivateKey(privateKeyString: String): PrivateKey =
    PKCS8EncodedKeySpec(Base64.getDecoder().decode(privateKeyString))
      .let { KeyFactory.getInstance("RSA").generatePrivate(it) }

  private fun readPublicKey(publicKeyString: String): PublicKey =
    X509EncodedKeySpec(Base64.getDecoder().decode(publicKeyString))
      .let { KeyFactory.getInstance("RSA").generatePublic(it) }

  fun generateToken(email: String): String =
    Jwts.builder()
      .setId(UUID.randomUUID().toString())
      .setSubject(email)
      .setExpiration(Date.from(calculateExpiryAtMidnight(expiry)))
      .addClaims(
        mapOf(
          "authorities" to listOf("ROLE_SLM_CREATE_BARCODE"),
          "client_id" to "send-legal-mail",
          "user_name" to email,
        )
      )
      .signWith(SignatureAlgorithm.RS256, privateKey)
      .compact()

  private fun calculateExpiryAtMidnight(expiry: Duration) =
    Instant.now(clock)
      .plus(expiry.toMillis(), ChronoUnit.MILLIS)
      .plus(1, ChronoUnit.DAYS)
      .truncatedTo(ChronoUnit.DAYS)

  fun validateToken(jwt: String): Boolean =
    runCatching {
      Jwts.parser().setSigningKey(publicKey).parseClaimsJws(jwt)
    }.onFailure {
      if ((it is ExpiredJwtException).not()) {
        log.warn("Found an invalid JWT: $jwt", it)
      }
    }.isSuccess

  fun subject(jwt: String): String =
    Jwts.parser().setSigningKey(publicKey).parseClaimsJws(jwt).body.subject

  @Suppress("UNCHECKED_CAST")
  fun authorities(jwt: String): List<String>? =
    Jwts.parser().setSigningKey(publicKey).parseClaimsJws(jwt).body["authorities"] as? List<String>

  @Suppress("UNCHECKED_CAST")
  fun clientId(jwt: String): String? =
    Jwts.parser().setSigningKey(publicKey).parseClaimsJws(jwt).body["client_id"] as? String

  fun expiresAt(jwt: String): Instant =
    Jwts.parser().setSigningKey(publicKey).parseClaimsJws(jwt).body.expiration.toInstant()

  fun isNomisUserToken(authToken: String?) =
    authToken?.isNotBlank()
      ?.and((getClaimsFromJWT(authToken).getClaim("user_name") as String?).isNullOrBlank().not())
      ?: false

  @Throws(ParseException::class)
  private fun getClaimsFromJWT(token: String): JWTClaimsSet =
    SignedJWT.parse(token.replace("Bearer ", "")).jwtClaimsSet
}
