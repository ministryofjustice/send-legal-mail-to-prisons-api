package uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.security

import org.springframework.beans.factory.annotation.Value
import org.springframework.cache.CacheManager
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.AuthenticationProvider
import org.springframework.security.authentication.ProviderManager
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.invoke
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.core.userdetails.UserDetailsByNameServiceWrapper
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationProvider
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken
import org.springframework.security.web.authentication.preauth.RequestHeaderAuthenticationFilter

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true, proxyTargetClass = true)
class ResourceServerConfiguration(private val barcodeUserDetailsService: UserDetailsService) {

  @Bean
  @Throws(Exception::class)
  fun filterChain(http: HttpSecurity): SecurityFilterChain {
    http {
      headers { frameOptions { sameOrigin = true } }
      sessionManagement { sessionCreationPolicy = SessionCreationPolicy.STATELESS }
      // Can't have CSRF protection as requires session
      csrf { disable() }
      addFilterAfter<RequestHeaderAuthenticationFilter>(createBarcodeAuthenticationFilter())
      authorizeHttpRequests {
        listOf(
          "/health/**",
          "/info",
          "/swagger-resources/**",
          "/v3/api-docs/**",
          "/swagger-ui/**",
          "/swagger-ui.html",
          "/cjsm/directory/refresh", // Protected by the ingress - see Kube config in helm_deploy
          "/barcode-stats-report", // Protected by the ingress - see Kube config in helm_deploy
          "/prisons",
        ).forEach { authorize(it, permitAll) }
        authorize(anyRequest, authenticated)
      }
      oauth2ResourceServer { jwt { jwtAuthenticationConverter = AuthAwareTokenConverter() } }
    }
    return http.build()
  }

  @Bean
  fun createBarcodeAuthenticationFilter(): RequestHeaderAuthenticationFilter =
    RequestHeaderAuthenticationFilter()
      .apply {
        setPrincipalRequestHeader("Create-Barcode-Token")
        setAuthenticationManager(authenticationManager())
        setExceptionIfHeaderMissing(false)
      }

  @Bean
  fun authenticationManager(): AuthenticationManager =
    ProviderManager(mutableListOf<AuthenticationProvider>(preAuthProvider()))

  @Bean
  fun preAuthProvider(): PreAuthenticatedAuthenticationProvider =
    PreAuthenticatedAuthenticationProvider()
      .apply { setPreAuthenticatedUserDetailsService(userDetailsServiceWrapper()) }

  @Bean
  fun userDetailsServiceWrapper(): UserDetailsByNameServiceWrapper<PreAuthenticatedAuthenticationToken> =
    UserDetailsByNameServiceWrapper<PreAuthenticatedAuthenticationToken>()
      .apply { setUserDetailsService(barcodeUserDetailsService) }

  @Bean
  fun locallyCachedJwtDecoder(
    @Value("\${spring.security.oauth2.resourceserver.jwt.jwk-set-uri}") jwkSetUri: String,
    cacheManager: CacheManager,
  ): JwtDecoder = NimbusJwtDecoder.withJwkSetUri(jwkSetUri).cache(cacheManager.getCache("jwks")).build()
}
