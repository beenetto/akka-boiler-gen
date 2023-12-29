package *module_name*.actors

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import *module_name*.tasks.Task


class TaskActor extends Actor with ActorLogging {

  override def receive: Receive = {
    case task: Task =>
      log.info(s"Received task: $task")

      val resultActor = context.actorOf(Props[ProcessorActor])
      resultActor ! task
      
  }

}
