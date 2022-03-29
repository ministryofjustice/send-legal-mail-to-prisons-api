package uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.mocks

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.VerificationException
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor
import com.github.tomakehurst.wiremock.common.SingleRootFileSource
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import org.junit.jupiter.api.extension.AfterAllCallback
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext

class PrisonerSearchExtension : BeforeAllCallback, AfterAllCallback, BeforeEachCallback {

  companion object {
    @JvmField
    val prisonerSearchApi = PrisonerSearchMockServer()
  }

  override fun beforeAll(context: ExtensionContext) {
    prisonerSearchApi.start()
  }

  override fun beforeEach(context: ExtensionContext) {
    prisonerSearchApi.resetRequests()
  }

  override fun afterAll(context: ExtensionContext) {
    prisonerSearchApi.stop()
  }
}

class PrisonerSearchMockServer : WireMockServer(WIREMOCK_CONFIG) {
  companion object {
    private const val WIREMOCK_PORT = 8091
    private val WIREMOCK_CONFIG = WireMockConfiguration.wireMockConfig()
      .port(WIREMOCK_PORT)
      .fileSource(SingleRootFileSource("src/testIntegration/resources"))
  }

  fun stubMatchPrisoners() {
    stubFor(
      post(WireMock.urlEqualTo("/match-prisoners"))
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withBodyFile("prisoner-search/match-prisoners-response.json")
        )
    )
  }

  fun stubGlobalSearch() {
    stubFor(
      post(WireMock.urlEqualTo("/global-search"))
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withBodyFile("prisoner-search/global-search-response.json")
        )
    )
  }

  fun matchPrisonersHasBeenCalled(): Boolean =
    try {
      verify(postRequestedFor(WireMock.urlEqualTo("/match-prisoners")))
      true
    } catch (verificationException: VerificationException) {
      false
    }

  fun globalSearchHasBeenCalled(): Boolean =
    try {
      verify(postRequestedFor(WireMock.urlEqualTo("/global-search")))
      true
    } catch (verificationException: VerificationException) {
      false
    }
}
