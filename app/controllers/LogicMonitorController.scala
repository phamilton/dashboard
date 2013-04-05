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


object LogicMonitorController extends Controller {
  
  def index = Action { implicit request =>
    val myActor = Akka.system.actorFor("/user/logicMonitor")
    AsyncResult {
      implicit val timeout = Timeout(30 seconds)
      (myActor ? GetStatus).mapTo[LogicMonitorStatus].map { status => 
        Ok(views.html.logicMonitor())
      }
    }
  }
  
  def refresh = Action { implicit request =>
    val myActor = Akka.system.actorFor("/user/logicMonitor")
    AsyncResult {
      implicit val timeout = Timeout(30 seconds)
      (myActor ? GetStatus).mapTo[LogicMonitorStatus].map { status => 
        Ok(status.results.trim.replaceAll("\"Nan\"", "0"))
      }
    }
  }
  
  def sockHandler = WebSocket.async[String] { request => 
    val myActor = Akka.system.actorFor("/user/logicMonitor")
    implicit val timeout = Timeout(30 seconds)
    
    (myActor ? Subscribe).map {
      case LMWebSocketResponse(in, out) =>
        (in, out)
    }      
  }
}