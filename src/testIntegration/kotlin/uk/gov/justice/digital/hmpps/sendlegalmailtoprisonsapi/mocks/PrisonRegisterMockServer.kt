package uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.mocks

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import org.junit.jupiter.api.extension.AfterAllCallback
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext

class PrisonRegisterExtension :
  BeforeAllCallback,
  AfterAllCallback,
  BeforeEachCallback {

  companion object {
    @JvmField
    val prisonRegisterApi = PrisonRegisterMockServer()
  }

  override fun beforeAll(context: ExtensionContext) {
    prisonRegisterApi.start()
  }

  override fun beforeEach(context: ExtensionContext) {
    prisonRegisterApi.resetRequests()
  }

  override fun afterAll(context: ExtensionContext) {
    prisonRegisterApi.stop()
  }
}

class PrisonRegisterMockServer : WireMockServer(WIREMOCK_PORT) {
  companion object {
    private const val WIREMOCK_PORT = 8092
  }

  fun stubGetPrisonOk(prisonCode: String) {
    stubFor(
      get(urlEqualTo("/prisons/id/$prisonCode"))
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withBody(
              """{
              "prisonId": "$prisonCode",
              "prisonName": "any name",
              "active": true,
              "male": true,
              "female": true,
              "contracted": false,
              "types": [{ "code": "HMP", "description": "HMP" }],
              "addresses": []
            }""",
            ),
        ),
    )
  }

  fun stubGetPrisonNotFound(prisonCode: String) {
    stubFor(
      get(urlEqualTo("/prisons/id/$prisonCode"))
        .willReturn(aResponse().withStatus(404)),
    )
  }
}
