
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

