package models

import akka.actor.Actor
import dispatch._
import scala.concurrent.Future
import akka.pattern.pipe
import play.api.libs.json.Json
import scala.concurrent.duration._
import akka.actor.ActorRef

case object UpdateStatus
case object GetStatus
case object NewStatus

case class Result(value: String)


sealed trait Status
case object Failing extends Status
case object Warning extends Status
case object Passing extends Status
case object Disabled extends Status

case class JobStatus(job: JenkinsJob, status: Status)

class JenkinsJobActor(job: JenkinsJob) extends Actor {
  import context._
  
  private var status = getStatus
  
  override def preStart() {
    system.scheduler.schedule(10 seconds, 10 seconds, self, UpdateStatus)
  }
    
  def receive = {
    case UpdateStatus => {        
        val newStatus = getStatus
        if (newStatus != status) {          
          status = newStatus
          parent ! NewStatus
        }       
    }
    case GetStatus => {  
      sender ! status
    }
  }
  
  private def getStatus(): JobStatus = {
    val svc = url(job.url + "/api/json")
      val results = Http(svc OK as.String)
      val rrr = results.map { r =>
          val jsValue = Json.parse(r)
          val color = (jsValue \ "color").as[String]
          val status = color match {
            case "blue" => JobStatus(job, Passing)
            case "green" => JobStatus(job, Passing)
            case "red" => JobStatus(job, Failing)
            case "disabled" => JobStatus(job, Disabled)
            case _ => JobStatus(job, Failing)
          }
          status
    }
    rrr.apply
  }
}