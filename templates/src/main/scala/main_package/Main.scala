package *module_name*

import kamon.Kamon
import akka.actor.{ActorSystem, Props}
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives._
import com.typesafe.config.{Config, ConfigFactory}
import org.slf4j.{Logger, LoggerFactory}
import processor.cdn.CloudFrontInvalidator
import processor.consumer.SQSConsumer
import utils.{ServiceRoutes, ConfigFactorySingleton}

import scala.concurrent.Future
import scala.concurrent.{Await, Promise}
import scala.concurrent.duration._
import scala.util.control.NonFatal
import scala.util.{Failure, Success}

object Main extends App {

  private val config         = ConfigFactorySingleton.loadEnvironmentConfig()
  private val logger: Logger = LoggerFactory.getLogger(getClass)

  private implicit val system: ActorSystem = ActorSystem("*module_name*", config)

  private val cloudFrontInvalidator = system.actorOf(Props[CloudFrontInvalidator], "cloudFrontInvalidator")

  private val sqsConsumer =
    system.actorOf(Props(new SQSConsumer(config.getString("sqs.queue.url"), cloudFrontInvalidator)), "sqsConsumer")

  private val maxAttempts          = 3
  private val delayBetweenAttempts = 5.seconds

  try {
    init()
    Kamon.init()
  } catch {
    case NonFatal(e) =>
      logger.error("Terminating due to initialization failure.", e)
      system.terminate()
  }

  private def init()(implicit system: ActorSystem): Unit = {
    var attempt                  = 0
    var initializationSuccessful = false

    while (attempt < maxAttempts && !initializationSuccessful) {
      attempt += 1

      logger.info(s"Initializing cdn-invalidation-processor service (Attempt $attempt)")

      initializationSuccessful = tryInitialize(attempt)

      if (!initializationSuccessful && attempt < maxAttempts) {
        logger.info(s"Delaying for $delayBetweenAttempts before the next attempt.")
        system.scheduler.scheduleOnce(delayBetweenAttempts) {
          init()
        }(system.dispatcher)
      }
    }
  }

  private def tryInitialize(attempt: Int)(implicit system: ActorSystem): Boolean = {
    val serviceConfig = config.getConfig("service")
    val host          = serviceConfig.getString("host")
    val port          = serviceConfig.getInt("port")

    val routes = ServiceRoutes(() => isReady)

    val bindingFuture: Future[Http.ServerBinding] = Http().newServerAt(host, port).bind(routes)

    try {
      bindingFuture.onComplete {
        case Success(_)  =>
          logger.info(s"Service endpoints [/alive, /ready] are available @ http://$host:$port/")
          if (isReady) {
            sqsConsumer ! Start(
              config.getConfig("sqs").getInt("consumer.maxNumberOfMessages"),
              config.getConfig("sqs").getInt("consumer.ratePerSecond")
            )
          }
        case Failure(ex) =>
          logger.error(s"Failed to bind service to $host:$port (Attempt $attempt)", ex)
      }(system.dispatcher)

      // Return true on success
      true
    } catch {
      case NonFatal(ex) =>
        logger.error(s"Failed to bind service to $host:$port (Attempt $attempt)", ex)
        // Return false on failure
        false
    }
  }

  private def isReady: Boolean = {
    val sqsReady        = SQSConsumer.isReady(config.getString("sqs.queue.url"))
    val cloudFrontReady = CloudFrontInvalidator.isReady
    sqsReady && cloudFrontReady
  }
}
