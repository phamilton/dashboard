import play.api._
import play.api.libs.concurrent.Akka
import akka.actor.Props
import models._
import play.api.Play.current
import com.typesafe.config.ConfigFactory


object Global extends GlobalSettings {  
  override def onStart(app: Application) {
    val jenkinsConfig = ConfigFactory.load("jenkins.properties").getConfig("jenkins");
    val logicMonitorConfig = ConfigFactory.load("logicMonitor.properties").getConfig("logicMonitor");
    
    val actor = Akka.system.actorOf(Props(new JenkinsActor(jenkinsConfig.getString("url"), jenkinsConfig.getString("dashboard"))), name = "jenkins")
    actor ! Refresh
    
    Akka.system.actorOf(Props(new LogicMonitorActor(logicMonitorConfig.getString("url"))), name = "logicMonitor")
  }
}