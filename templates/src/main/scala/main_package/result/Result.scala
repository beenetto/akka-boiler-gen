package *module_name*.result

import scala.util.{Success, Failure, Try}
import scala.Throwable

case class Result[T](value: Try[T])

object Result {

  def apply[T](result: T): Result[T] = {
    new Result(Success(result))
  }

  def apply[T](error: Throwable): Result[T] = {
    new Result(Failure(error))
  }

}
