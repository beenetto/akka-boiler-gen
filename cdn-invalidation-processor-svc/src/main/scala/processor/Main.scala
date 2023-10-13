
package processor

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory

import scala.util.control.NonFatal

import akka.actor.{ ActorSystem, Props }
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives._
import utils.ServiceRoutes

object Main extends App {
  private lazy val config = ConfigFactory.load()
  // Kamon.init(config)
  private val logger = LoggerFactory.getLogger(getClass)
  implicit val system: ActorSystem = ActorSystem("processor", config)

  try {
    init(config, logger)
  } catch {
    case NonFatal(e) =>
      logger.error("Terminating due to initialization failure.", e)
      system.terminate()
  }

  private def init(config: Config, logger: Logger)(using system: ActorSystem): Unit =
    logger.info("Initializing *project_name* service endpoints")
    val serviceConf = config.getConfig("service")
    val host = serviceConf.getString("host")
    val port = serviceConf.getInt("port")
    val bindingFuture = Http().newServerAt(host, port).bind(ServiceRoutes(() => isReady))
    logger.info(s"Service endpoints [/alive, /ready] are available @ http://$host:$port/")

  def isReady: Boolean = {
    checkOne() && checkTwo()
  }

  // readiness checks
  private def checkOne(): Boolean = true
  private def checkTwo(): Boolean = true
}

