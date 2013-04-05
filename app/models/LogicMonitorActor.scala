package models

import akka.actor.Actor
import dispatch._
import play.api.libs.json._
import play.api.libs.iteratee._
import scala.concurrent.duration._

case class LogicMonitorStatus(results: String)

case class DataPoint(ts: String, dt: String, value: String)
case class LogicMonitorResult(value: Int)

case class LMWebSocketResponse(in: Iteratee[String,_], out: Enumerator[String])

class LogicMonitorActor(serviceUrl: String) extends Actor {
  import context._    
  
  private val (out, channel) = Concurrent.broadcast[String]
  private val in = Iteratee.foreach[String] { println(_) }.mapDone { println(_) } 
  
  private var status = getStatus
  
  override def preStart() {
    system.scheduler.schedule(30 seconds, 30 seconds, self, UpdateStatus)
  }
    
  def receive = {
    case GetStatus => {
      sender ! status
    }
    case UpdateStatus => {      
      val newStatus = getStatus()
      if (newStatus != status) {
        status = newStatus        
        channel.push(status.results)
      }
    }    
    case Subscribe => {      
      sender ! LMWebSocketResponse(in, out)
    }
  }
  
  private def getStatus(): LogicMonitorStatus = {  
      val svc = url(serviceUrl)
      val results = Http(svc OK as.String)
      val rrr = results.map { r =>
        LogicMonitorStatus(r)     
      }
      rrr.apply  
  }
}