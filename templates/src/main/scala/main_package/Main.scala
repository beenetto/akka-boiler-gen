package *module_name*

import kamon.Kamon
import akka.actor.{ActorSystem, Props}
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives._
import com.typesafe.config.{Config, ConfigFactory}
import org.slf4j.{Logger, LoggerFactory}

import utils.{ServiceRoutes, ConfigFactorySingleton}

import scala.concurrent.Future
import scala.concurrent.{Await, Promise}
import scala.concurrent.duration._
import scala.util.control.NonFatal
import scala.util.{Failure, Success}

import *module_name*.tasks.Task
import *module_name*.actors.{TaskActor, ProcessorActor}


object Main extends App {

  private val appConfig = ConfigFactorySingleton.loadEnvironmentConfig()
  private val logger = LoggerFactory.getLogger(getClass)

  private implicit val actorSystem: ActorSystem = ActorSystem("*module_name*", appConfig)

  private val taskActor = actorSystem.actorOf(Props[TaskActor], "taskActor")
  private val resultActor = actorSystem.actorOf(Props[ProcessorActor], "resultActor")

  private val maxInitAttempts = 3
  private val initRetryDelay = 5.seconds

  try {
    initialize()

  } catch {
    case NonFatal(e) =>
      logger.error("Terminating due to initialization failure.", e)
      actorSystem.terminate()
  }

  private def initialize()(implicit system: ActorSystem): Unit = {
    var initAttempts = 0
    var isInitialized = false

    while (initAttempts < maxInitAttempts && !isInitialized) {
      initAttempts += 1

      logger.info(s"Initializing cdn-invalidation-processor service (Attempt $initAttempts)")

      isInitialized = tryInitialize(initAttempts)

      if (!isInitialized && initAttempts < maxInitAttempts) {
        logger.info(s"Delaying for $initRetryDelay before the next attempt.")
        system.scheduler.scheduleOnce(initRetryDelay) {
          initialize()
        }(system.dispatcher)
      }
    }
  }

  private def tryInitialize(attempt: Int)(implicit system: ActorSystem): Boolean = {
    val serviceConfig = appConfig.getConfig("service")
    val host = serviceConfig.getString("host")
    val port = serviceConfig.getInt("port")

    val routes = ServiceRoutes(() => isReady)

    val bindingFuture: Future[Http.ServerBinding] = Http().newServerAt(host, port).bind(routes)

    try {
      bindingFuture.onComplete {
        case Success(_) =>
          logger.info(s"Service endpoints [/alive, /ready] are available @ http://$host:$port/")
          Kamon.init()
          taskActor ! Task("1", "data") // TODO: implement Start task
        case Failure(ex) =>
          logger.error(s"Failed to bind service to $host:$port (Attempt $attempt)", ex)
      }(system.dispatcher)
      true
    } catch {
      case NonFatal(ex) =>
        logger.error(s"Failed to bind service to $host:$port (Attempt $attempt)", ex)
        false
    }
  }

  private def isReady: Boolean = {
    true // TODO: Implement this
  }
}
