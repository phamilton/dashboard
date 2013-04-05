package models

import akka.actor.Actor
import akka.actor.ActorRef
import dispatch._
import scala.concurrent.Future
import akka.pattern.pipe
import play.api.libs.json._
import scala.concurrent.duration._
import akka.actor.Props
import java.net.URLEncoder
import akka.pattern.ask
import akka.util.Timeout
import play.api.libs.concurrent._
import play.api.libs.iteratee._

case class JenkinsJob(name: String, url: String, color: String)
case class JenkinsDashboard(name: String, url: String, jobs: List[JenkinsJob])
case class WebSocketResponse(in: Iteratee[JsValue,_], out: Enumerator[JsValue])

case object Refresh
case object Subscribe

class JenkinsActor(baseUrl: String, dashboardName: String) extends Actor {
  import context._
  
  implicit val jobStatusWrites  = new Writes[JobStatus] {
      def writes(c: JobStatus): JsValue = {
        Json.obj(
         "name" -> c.job.name,
         "status" -> c.status.toString
        )
      }
  }
  
  private var dashboard = getDashboard()
  private var jobActors = List[ActorRef]()
  
  private val (out, channel) = Concurrent.broadcast[JsValue]
  private val in = Iteratee.foreach[JsValue] { println(_) }.mapDone { println(_) } 
  
  
   implicit object JenkinsJobReads extends Reads[JenkinsJob] {
    def reads(json: JsValue): JsResult[JenkinsJob] = {
      JsSuccess(JenkinsJob(
        (json \ "name").as[String],
        (json \ "url").as[String],
        (json \ "color").as[String]                                   
      ))
    }
  }
  
  implicit object JenkinsDashboardReads extends Reads[JenkinsDashboard] {
      def reads(json: JsValue): JsResult[JenkinsDashboard] = {
        JsSuccess(JenkinsDashboard(
            (json \ "name").as[String],
            (json \ "url").as[String],         
            (json \ "jobs").as[List[JenkinsJob]]
        ))        
      }       
  } 

  def receive = {
    case Refresh => {
      dashboard = getDashboard()
      jobActors = dashboard.jobs.map { job =>
        context.actorOf(Props(new JenkinsJobActor(job)), name = URLEncoder.encode(job.name))                
      }
    }
    case GetStatus => {
      implicit val timeout = Timeout(10 seconds)
      val fJobStatuses = jobActors.map {
        _ ? GetStatus
      }
      Future.sequence(fJobStatuses) pipeTo sender
    }
    case NewStatus => {
      implicit val timeout = Timeout(10 seconds)
      val fJobStatuses = jobActors.map {
        _ ? GetStatus
      }
      Future.sequence(fJobStatuses).mapTo[List[JobStatus]].map { statuses =>        
        channel.push(Json.toJson(statuses))
      }
    }    
    case Subscribe => {      
      sender ! WebSocketResponse(in, out)
    }
  }
  
  private def getDashboard(): JenkinsDashboard = {
    val dashboardUrl = url(baseUrl + dashboardName + "/api/json")    
    val results = Http(dashboardUrl OK as.String)
    val jdFuture = results.map { r =>  
      Json.parse(r).as[JenkinsDashboard]
    }
    jdFuture.apply
  }  
}