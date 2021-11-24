package uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.security

import io.jsonwebtoken.ExpiredJwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import mu.KotlinLogging
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.config.JwtConfig
import java.security.KeyFactory
import java.security.PrivateKey
import java.security.PublicKey
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import java.time.Duration
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.Base64
import java.util.Date
import java.util.UUID

private val log = KotlinLogging.logger {}

@Service
class JwtService(jwtConfig: JwtConfig) {

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
      .setExpiration(Date.from(Instant.now().plus(expiry.toMillis(), ChronoUnit.MILLIS)))
      .addClaims(mapOf("authorities" to arrayOf("ROLE_SLM_CREATE_BARCODE")))
      .signWith(SignatureAlgorithm.RS256, privateKey)
      .compact()

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
}
