package controllers

import play.api._
import play.api.mvc._
import akka.actor.Props
import play.api.libs.concurrent.Akka
import play.api.libs.iteratee._
import play.api.Play.current
import models._
import akka.pattern.ask
import akka.util.Timeout
import scala.concurrent.duration._
import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.json._


object JenkinsController extends Controller {
  
  implicit val jobStatusWrites  = new Writes[JobStatus] {
      def writes(c: JobStatus): JsValue = {
        Json.obj(
         "name" -> c.job.name,
         "status" -> c.status.toString
        )
      }
  }
  
  def index = Action { implicit request =>
    val myActor = Akka.system.actorFor("/user/jenkins")
    AsyncResult {
      implicit val timeout = Timeout(30 seconds)
      (myActor ? GetStatus).mapTo[List[JobStatus]].map { statuses => 
        Ok(views.html.jenkins())
      }
    }
  }
  
  def refresh = Action { implicit request =>
    val myActor = Akka.system.actorFor("/user/jenkins")
    AsyncResult {
      implicit val timeout = Timeout(30 seconds)
      (myActor ? GetStatus).mapTo[List[JobStatus]].map { statuses => 
        Ok(Json.toJson(statuses))
      }
    }
  }
  
  def sockHandler = WebSocket.async[JsValue] { request => 
    val myActor = Akka.system.actorFor("/user/jenkins")
    implicit val timeout = Timeout(30 seconds)
    
    (myActor ? Subscribe).map {
      case WebSocketResponse(in, out) =>
        (in, out)
    }    
  
  }
}