package uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.security

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.AuthenticationProvider
import org.springframework.security.authentication.ProviderManager
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.core.userdetails.UserDetailsByNameServiceWrapper
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationProvider
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken
import org.springframework.security.web.authentication.preauth.RequestHeaderAuthenticationFilter

@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true, proxyTargetClass = true)
class ResourceServerConfiguration(private val barcodeUserDetailsService: UserDetailsService) :
  WebSecurityConfigurerAdapter() {

  override fun configure(http: HttpSecurity) {
    http
      .sessionManagement()
      .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
      .and().headers().frameOptions().sameOrigin()
      .and().csrf().disable()
      .addFilterAfter(createBarcodeAuthenticationFilter(), RequestHeaderAuthenticationFilter::class.java)
      .authorizeRequests { auth ->
        auth.antMatchers(
          "/webjars/**",
          "favicon.ico",
          "/health/**",
          "/info",
          "/swagger-resources/**",
          "/v3/api-docs/**",
          "/swagger-ui/**",
          "/swagger-ui.html",
        ).permitAll().anyRequest().authenticated()
      }.oauth2ResourceServer().jwt().jwtAuthenticationConverter(AuthAwareTokenConverter())
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
  override fun authenticationManager(): AuthenticationManager =
    ProviderManager(mutableListOf<AuthenticationProvider>(preAuthProvider()))

  @Bean
  fun preAuthProvider(): PreAuthenticatedAuthenticationProvider =
    PreAuthenticatedAuthenticationProvider()
      .apply { setPreAuthenticatedUserDetailsService(userDetailsServiceWrapper()) }

  @Bean
  fun userDetailsServiceWrapper(): UserDetailsByNameServiceWrapper<PreAuthenticatedAuthenticationToken> =
    UserDetailsByNameServiceWrapper<PreAuthenticatedAuthenticationToken>()
      .apply { setUserDetailsService(barcodeUserDetailsService) }
}
