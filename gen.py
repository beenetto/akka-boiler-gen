import os
import shutil

SBT_CONTENT = """
name := "*project_name*"
version := "1.0.0"
scalaVersion := "3.1.1"

val akkaVersion = "2.6.20"
val akkaHttpVersion = "10.2.10"

// Logging
libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.4.7"
libraryDependencies += "ch.qos.logback.contrib" % "logback-jackson" % "0.1.5"
libraryDependencies += "ch.qos.logback.contrib" % "logback-json-classic" % "0.1.5"
libraryDependencies += "io.sentry" % "sentry-logback" % "6.19.0"

// AKKA
libraryDependencies += "com.typesafe.akka" %% "akka-slf4j" % akkaVersion cross CrossVersion.for3Use2_13
libraryDependencies += "com.typesafe.akka" %% "akka-actor-typed" %  akkaVersion cross CrossVersion.for3Use2_13
libraryDependencies += "com.typesafe.akka" %% "akka-stream" % akkaVersion cross CrossVersion.for3Use2_13

// AKKA http
libraryDependencies += "com.typesafe.akka" %% "akka-http" % akkaHttpVersion cross CrossVersion.for3Use2_13
libraryDependencies += "com.typesafe.akka" %% "akka-http-spray-json" % akkaHttpVersion cross CrossVersion.for3Use2_13


"""

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
    logger.info(s"Service endpoints ["/alive", "/ready"] are available @ http://$host:$port/")

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

APPLICATION_CONF_CONTENT = """
include "service"
akka {
    loglevel = "INFO"
}

"""

SERVICE_CONF_CONTENT = """
service {
    host = "localhost"
    port = 8558
    readiness = "ready"
    health = "alive"
}

"""


def generate_akka_project(project_name, k8s_namespace):
    """Generates an Akka project with the given name."""

    # Create the project directory
    project_dir = os.path.join(os.getcwd(), project_name.strip())
    os.makedirs(project_dir, exist_ok=True)

    # Create the source directory
    source_dir = os.path.join(project_dir, "src", "main", "scala", module_name.strip())
    os.makedirs(source_dir, exist_ok=True)

    # Create the source directory
    utils_dir = os.path.join(project_dir, "src", "main", "scala", "utils")
    os.makedirs(utils_dir, exist_ok=True)

    # Create the resources directory
    resources_dir = os.path.join(project_dir, "src", "main", "resources")
    os.makedirs(resources_dir, exist_ok=True)

    # Copy K8s
    k8s_dir = os.path.join(project_dir, "k8s")
    shutil.copytree("templates/k8s",  k8s_dir)

    for root, dirs, files in os.walk(k8s_dir):
        for file in files:
            file_path = os.path.join(root, file)
            with open(file_path, "r") as in_file:
                file_contents = in_file.read()
                modified_contents = file_contents.replace(
                    "*project_name*", project_name.strip()).replace(
                    "*k8s_namespace*", k8s_namespace.strip())
                with open(file_path, "w") as output_file:
                    output_file.write(modified_contents)

    # Copy static files
    shutil.copyfile(
        "templates/static/logback.xml",
        os.path.join(resources_dir, "logback.xml"))

    # Create the SBT file
    sbt_file = os.path.join(project_dir, "build.sbt")
    with open(sbt_file, "w") as f:
        f.write(SBT_CONTENT.replace('*project_name*', project_name.strip()))

    # Create the main class
    main_class_file = os.path.join(source_dir, "Main.scala")
    with open(main_class_file, "w") as f:
        f.write(MAIN_CLASS_CONTENT.replace('*module_name*', module_name.strip()))

    # Create the service routes
    service_routes_file = os.path.join(utils_dir, "ServiceRoutes.scala")
    with open(service_routes_file, "w") as f:
        f.write(SERVICE_ROUTES_CONTENT.replace('*module_name*', module_name.strip()))

    # Create the application configuration file
    config_file = os.path.join(resources_dir, "application.conf")
    with open(config_file, "w") as f:
        f.write(APPLICATION_CONF_CONTENT)

    # Create the service configuration file
    config_file = os.path.join(resources_dir, "service.conf")
    with open(config_file, "w") as f:
        f.write(SERVICE_CONF_CONTENT)


if __name__ == "__main__":
    project_name = input("Enter the name of the Akka project: ")
    module_name = input("Enter the name of the Akka module: ")
    k8s_namespace = input("Enter the namespace of the Akka module: ")

    print(f"Project name: {project_name}")
    print(f"Module name: {module_name}")
    print(f"k8s namespace: {k8s_namespace}")

    confirmation = input(f"Proceed with creating project ${project_name}? (y/n): ")

    if confirmation == "y":
        generate_akka_project(project_name, k8s_namespace)
        print(f"${project_name} created successfully.")
    else:
        print("Files not copied.")
