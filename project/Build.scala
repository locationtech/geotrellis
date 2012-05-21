import sbt._
import sbt.Keys._

import com.typesafe.startscript.StartScriptPlugin

object MyBuild extends Build {
  val geotoolsVersion = "8.0-M4"

  lazy val root = Project("root", file(".")) settings(
    organization := "azavea",
    name := "geotrellis",
    version := "0.6",
    scalaVersion := "2.9.1",

    scalacOptions ++= Seq("-deprecation", "-unchecked", "-optimize"),
    parallelExecution := false,
    testListeners <+= target.map(tgt => new eu.henkelmann.sbt.JUnitXmlTestsListener(tgt.toString)),

    mainClass := Some("geotrellis.rest.WebRunner"),

    StartScriptPlugin.stage in Compile := Unit,

    javaOptions in run += "-Xmx4G",

    libraryDependencies ++= Seq(
      "org.scalatest" %% "scalatest" % "1.6.1" % "test",
      "junit" % "junit" % "4.5" % "test",
      "com.vividsolutions" % "jts" % "1.8",
      "java3d" % "j3d-core" % "1.3.1",
      "org.geotools" % "gt-main" % geotoolsVersion,
      "org.geotools" % "gt-jdbc" % geotoolsVersion,
      "org.geotools.jdbc" % "gt-jdbc-postgis" % geotoolsVersion,
      "org.geotools" % "gt-coverage" % geotoolsVersion,
      "org.geotools" % "gt-coveragetools" % geotoolsVersion,
      "org.postgis" % "postgis-jdbc" % "1.3.3",
      "javax.media" % "jai_core" % "1.1.3",
      "postgresql" % "postgresql" % "8.4-701.jdbc4",
      "net.liftweb" %% "lift-json" % "2.4-M5",
      "com.typesafe.akka" % "akka-kernel" % "2.0.1",
      "com.typesafe.akka" % "akka-remote" % "2.0.1",
      "com.typesafe.akka" % "akka-actor"  % "2.0.1",
      "org.eclipse.jetty" % "jetty-webapp" % "8.1.0.RC4",
      "com.sun.jersey" % "jersey-bundle" % "1.11",
      "com.azavea.math" %% "numeric" % "0.1" from "http://plastic-idolatry.com/jars/numeric_2.9.1-0.1.jar",
      "com.azavea.math.plugin" %% "optimized-numeric" % "0.1" from "http://plastic-idolatry.com/jars/optimized-numeric-plugin_2.9.1-0.1.jar",
      //"com.beust" % "jcommander" % "1.23", 
      "org.reflections" % "reflections" % "0.9.5",
      "org.slf4j" % "slf4j-api" % "1.6.0",
      "org.slf4j" % "slf4j-nop" % "1.6.0"
    ),

    resolvers ++= Seq(
      "Geotools" at "http://download.osgeo.org/webdav/geotools/", 
      "NL4J Repository" at "http://nativelibs4java.sourceforge.net/maven/",
      "maven2 dev repository" at "http://download.java.net/maven/2",
      "Scala Test" at "http://www.scala-tools.org/repo-reloases/",
      "Typesafe Repo" at "http://repo.typesafe.com/typesafe/releases/",
      "Local Maven Repository" at "file://"+Path.userHome.absolutePath+"/.m2/repository",
      "sonatypeSnapshots" at "http://oss.sonatype.org/content/repositories/snapshots"
    )
  )


  lazy val tasks: Project = Project("tasks", file("tasks")) settings (tasksSettings: _*) dependsOn (root)

  def tasksSettings = Seq(
    libraryDependencies ++= Seq(
      "com.beust" % "jcommander" % "1.23"
    ),

    mainClass in Compile := Some("geotrellis.run.Tasks")

  )// ++ StartScriptPlugin.startScriptForClassesSettings

  lazy val benchmark: Project = Project("benchmark", file("benchmark")) settings (benchmarkSettings: _*) dependsOn (root)

  def benchmarkSettings = Seq(
    // raise memory limits here if necessary
    javaOptions in run += "-Xmx4G",

    libraryDependencies ++= Seq(
      "com.google.guava" % "guava" % "r09",
      "com.google.code.java-allocation-instrumenter" % "java-allocation-instrumenter" % "2.0",
      "com.google.code.caliper" % "caliper" % "1.0-SNAPSHOT" from "http://plastic-idolatry.com/jars/caliper-1.0-SNAPSHOT.jar",
      "com.google.code.gson" % "gson" % "1.7.1"
    ),

    // enable forking in run
    fork in run := true,

    // custom kludge to get caliper to see the right classpath
    
    // define the onLoad hook
    onLoad in Global <<= (onLoad in Global) ?? identity[State],
    {
      // attribute key to prevent circular onLoad hook
      val key = AttributeKey[Boolean]("loaded")
      val f = (s: State) => {
        val loaded: Boolean = s get key getOrElse false
        if (!loaded) {
          var cpString: String = ""
          // get the runtime classpath
          Project.evaluateTask(fullClasspath.in(Runtime), s) match {
            // make a colon-delimited string of the classpath
            case Some(Value(cp)) => cpString = cp.files.mkString(":")
            // probably should handle an error here, but not sure you can
            //  ever get here with a working sbt
            case _ => Nil
          }
          val extracted: Extracted = Project.extract(s)
          // return a state with loaded = true and javaOptions set correctly
          extracted.append(Seq(javaOptions in run ++= Seq("-cp", cpString)), s.put(key, true))
        } else {
          // return the state, unmodified
          s
        }
      }
      onLoad in Global ~= (f compose _)
    }
  )
}
