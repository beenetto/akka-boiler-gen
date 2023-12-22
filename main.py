import os
import shutil

MAIN_CLASS_CONTENT = """
package *module_name*

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
  implicit val system: ActorSystem = ActorSystem("*module_name*", config)

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

"""

SERVICE_ROUTES_CONTENT = """
package utils

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives.*
import akka.http.scaladsl.server.Route

object ServiceRoutes {
  def apply(isReady: () => Boolean): Route =
    path("") {
      complete(StatusCodes.Forbidden)
    } ~
      path("alive") {
        get {
          complete(StatusCodes.OK)
        }
      } ~
      path("ready") {
        get {
          if (isReady()) {
            complete(StatusCodes.OK)
          } else {
            complete(StatusCodes.InternalServerError, "Service is not ready.")
          }
        }
      }
}

"""


def generate_akka_project(project_name, k8s_namespace, project_owner, project_description):
    """Generates an Akka project with the given name."""

    project_dir = os.path.join(os.getcwd(), f'output/{project_name.strip()}')
    shutil.copytree("templates",  project_dir)

    for root, dirs, files in os.walk(project_dir):
        for file in files:
            file_path = os.path.join(root, file)
            with open(file_path, "r") as in_file:
                file_contents = in_file.read()
                modified_contents = file_contents.replace(
                    "*project_name*", project_name.strip()).replace(
                    "*k8s_namespace*", k8s_namespace.strip()).replace(
                    "*project_owner*", project_owner.strip()).replace(
                    "*project_description*", project_description.strip()).replace(
                    "*module_name*", module_name.strip())
                with open(file_path, "w") as output_file:
                    output_file.write(modified_contents)


if __name__ == "__main__":
    project_name = input("Enter the name of the Akka project: ")
    module_name = input("Enter the name of the Akka module: ")
    k8s_namespace = input("Enter K8s : ")
    project_owner = input("Enter the project owner: ") or ""
    project_description = input("Enter the project description: ") or ""

    print(f"Project name: {project_name}")
    print(f"Module name: {module_name}")
    print(f"K8s namespace: {k8s_namespace}")
    print(f"Owner: {project_owner}")
    print(f"Description: {project_description}")

    confirmation = input(f"Proceed with creating project ${project_name}? (y/n): ")

    if confirmation == "y":
        generate_akka_project(project_name, k8s_namespace, project_owner, project_description)
        print(f"${project_name} created successfully.")
    else:
        print("Files not copied.")
