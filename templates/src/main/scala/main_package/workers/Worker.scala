package *module_name*.workers

import *module_name*.Main.getClass
import org.slf4j.{Logger, LoggerFactory}

import scala.util.{Failure, Success}
import *module_name*.result.Result
import *module_name*.tasks.Task

class Worker {

  private val logger = LoggerFactory.getLogger(getClass)

  def process(task: Task): Result[String] = {
    logger.info(s"Processing task: $task")
    try {
      val result = doProcess(task)
      logger.info(s"Task processed successfully")
      Result(Success(result))
    } catch {
      case e: Exception =>
        logger.error(s"Error processing task: $task", e)
        Result(Failure(e))
    }
  }

  private def doProcess(task: Task): String = {
    // Actual task processing logic
    Thread.sleep(1000) // simulate long processing
    "processed result"
  }
  
}
