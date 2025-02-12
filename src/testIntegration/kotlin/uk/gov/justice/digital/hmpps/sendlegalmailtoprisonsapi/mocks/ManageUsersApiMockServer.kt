package uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.mocks

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.common.SingleRootFileSource
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import com.github.tomakehurst.wiremock.http.HttpHeader
import com.github.tomakehurst.wiremock.http.HttpHeaders
import org.junit.jupiter.api.extension.AfterAllCallback
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext

class ManageUsersApiExtension :
  BeforeAllCallback,
  AfterAllCallback,
  BeforeEachCallback {

  companion object {
    @JvmField
    val manageUsersApiMockServer = ManageUsersApiMockServer()
  }

  override fun beforeAll(context: ExtensionContext) {
    manageUsersApiMockServer.start()
    manageUsersApiMockServer.stubGetUserName()
    manageUsersApiMockServer.stubGetUserDetails()
  }

  override fun beforeEach(context: ExtensionContext) {
    manageUsersApiMockServer.resetRequests()
  }

  override fun afterAll(context: ExtensionContext) {
    manageUsersApiMockServer.stop()
  }
}

class ManageUsersApiMockServer : WireMockServer(WIREMOCK_CONFIG) {
  companion object {
    private const val WIREMOCK_PORT = 8093
    private val WIREMOCK_CONFIG = WireMockConfiguration.wireMockConfig()
      .port(WIREMOCK_PORT)
      .fileSource(SingleRootFileSource("src/testIntegration/resources"))
  }

  fun stubGetUserName() {
    stubFor(
      WireMock.get(WireMock.urlEqualTo("/users/me"))
        .willReturn(
          aResponse()
            .withHeaders(HttpHeaders(HttpHeader("Content-Type", "application/json")))
            .withBody(
              """
              {
                "username": "DUMMY_USER"
              }
              """.trimIndent(),
            ),
        ),
    )
  }

  fun stubGetUserDetails() {
    stubFor(
      WireMock.get(WireMock.urlEqualTo("/users/DUMMY_USER"))
        .willReturn(
          aResponse()
            .withHeaders(HttpHeaders(HttpHeader("Content-Type", "application/json")))
            .withBody(
              """
              {
                "activeCaseLoadId": "LEI"
              }
              """.trimIndent(),
            ),
        ),
    )
  }

  // If the user is not known then this endpoint still returns the username, just not the active caseload
  fun stubFailToGetUserDetails() {
    stubFor(
      WireMock.get(WireMock.urlEqualTo("/users/DUMMY_USER"))
        .willReturn(
          aResponse()
            .withHeaders(HttpHeaders(HttpHeader("Content-Type", "application/json")))
            .withBody(
              """
              {
                "username": "DUMMY_USER"
              }
              """.trimIndent(),
            ),
        ),
    )
  }

  fun stubHealthPing(status: Int) {
    stubFor(
      WireMock.get("/auth/health/ping").willReturn(
        aResponse()
          .withHeader("Content-Type", "application/json")
          .withBody(if (status == 200) "pong" else "some error")
          .withStatus(status),
      ),
    )
  }
}
