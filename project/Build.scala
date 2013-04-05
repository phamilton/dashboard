import sbt._
import Keys._
import play.Project._

object ApplicationBuild extends Build {

  val appName         = "dashboard"
  val appVersion      = "1.0-SNAPSHOT"

  val appDependencies = Seq(
    // Add your project dependencies here,
    jdbc,
    anorm,
    "com.codahale" % "jerkson_2.9.1" % "0.5.0",
    "net.databinder.dispatch" % "dispatch-core_2.10" % "0.9.5"
  )


  val main = play.Project(appName, appVersion, appDependencies).settings(
    // Add your own project settings here      
  )

}
