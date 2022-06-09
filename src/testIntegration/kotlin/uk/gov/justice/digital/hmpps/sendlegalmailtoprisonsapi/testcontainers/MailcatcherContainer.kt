package uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.testcontainers

import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.wait.strategy.Wait
import java.io.IOException
import java.net.ServerSocket

object MailcatcherContainer {
  val instance: GenericContainer<Nothing>? by lazy { startMailcatcherContainer() }
  private fun startMailcatcherContainer(): GenericContainer<Nothing>? =
    if (checkMailcatcherRunning().not()) {
      GenericContainer<Nothing>("sj26/mailcatcher").apply {
        withEnv("HOSTNAME_EXTERNAL", "localhost")
        withExposedPorts(1080, 1025)
        setWaitStrategy(Wait.forListeningPort())
        withReuse(true)
        start()
      }
    } else {
      null
    }

  private fun checkMailcatcherRunning(): Boolean =
    try {
      val serverSocket = ServerSocket(1080)
      serverSocket.localPort == 0
    } catch (e: IOException) {
      true
    }
}
