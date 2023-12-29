package *module_name*.actors

import scala.util.{Failure, Success, Try}
import akka.actor.{Actor, ActorLogging}

import *module_name*.tasks.Task
import *module_name*.result.Result
import *module_name*.workers.Worker

class ProcessorActor extends Actor with ActorLogging {

  private val worker = new Worker

  override def receive: Receive = {
    case task: Task =>
      log.info(s"ProcessorActor received task: $task")
      try {
        val result = worker.process(task)
        sender() ! result
      } catch {
        case e: Exception =>
          val result = Result(e)
          sender() ! result
      }
  }

}
