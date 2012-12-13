import sbt._
import sbt.Keys._

object MyBuild extends Build {
  val geotoolsVersion = "8.0-M4"

  val key = AttributeKey[Boolean]("javaOptionsPatched")

  lazy val root = Project("root", file(".")).settings(
    organization := "com.azavea.geotrellis",
    name := "geotrellis",
    version := "0.8.0-SNAPSHOT",
    scalaVersion := "2.10.0-RC3",
    
    scalacOptions ++= Seq("-deprecation", "-unchecked", "-optimize", "-language:implicitConversions", "-language:postfixOps", "-language:existentials", "-feature"),
    parallelExecution := false,
    testListeners <+= target.map(tgt => new eu.henkelmann.sbt.JUnitXmlTestsListener(tgt.toString)),

    mainClass := Some("geotrellis.rest.WebRunner"),

    javaOptions in run += "-Xmx2G",

    libraryDependencies ++= Seq(
      "org.scalatest" % "scalatest_2.10.0-RC3" % "1.8-B1" % "test",
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
      "net.liftweb" % "lift-json_2.10.0-RC2" % "2.5-SNAPSHOT" from "http://n0d.es/jars/lift-json_2.10.0-RC2.jar",
      // lift-json dependency included so we can get jar directly
      "com.thoughtworks.paranamer" % "paranamer" % "2.4.1",
      "com.typesafe.akka" % "akka-kernel_2.10.0-RC3" % "2.1.0-RC3",
      "com.typesafe.akka" % "akka-remote_2.10.0-RC3" % "2.1.0-RC3",
      "com.typesafe.akka" % "akka-actor_2.10.0-RC3" % "2.1.0-RC3",
      "org.eclipse.jetty" % "jetty-webapp" % "8.1.0.RC4",
      "com.sun.jersey" % "jersey-bundle" % "1.11",
      "com.google.code.java-allocation-instrumenter" % "java-allocation-instrumenter" % "2.0", 
      "org.reflections" % "reflections" % "0.9.5",
      "org.slf4j" % "slf4j-api" % "1.6.0",
      "org.slf4j" % "slf4j-nop" % "1.6.0",
      "org.codehaus.jackson" % "jackson-core-asl" % "1.6.1",
      "org.codehaus.jackson" % "jackson-mapper-asl" % "1.6.1"
    ),

    resolvers ++= Seq(
      "Geotools" at "http://download.osgeo.org/webdav/geotools/", 
      "NL4J Repository" at "http://nativelibs4java.sourceforge.net/maven/",
      "maven2 dev repository" at "http://download.java.net/maven/2",
      "Scala Test" at "http://www.scala-tools.org/repo-reloases/",
      "Typesafe Repo" at "http://repo.typesafe.com/typesafe/releases/",
      "Local Maven Repository" at "file://"+Path.userHome.absolutePath+"/.m2/repository",
      "sonatypeSnapshots" at "http://oss.sonatype.org/content/repositories/snapshots"
    ),

    // Settings to publish to Sonatype
    publishMavenStyle := true,
  
    publishTo <<= version { (v: String) => 
      val nexus = "https://oss.sonatype.org/"
      if (v.trim.endsWith("SNAPSHOT")) 
        Some("snapshots" at nexus + "content/repositories/snapshots") 
      else
        Some("releases"  at nexus + "service/local/staging/deploy/maven2")
    },

    publishArtifact in Test := false,
    pomIncludeRepository := { _ => false },
    licenses := Seq("GPL3" -> url("http://www.gnu.org/licenses/gpl-3.0-standalone.html")),
    homepage := Some(url("http://github.com/azavea/geotrellis")),

    pomExtra := (

<scm>
  <url>git@github.com:azavea/geotrellis.git</url>
  <connection>scm:git:git@github.com:azavea/geotrellis.git</connection>
</scm>
<developers>
  <developer>
    <id>non</id>
    <name>Erik Osheim</name>
    <url>http://github.com/non/</url>
  </developer>
  <developer>
    <id>josh.marcus</id>
    <name>Josh Marcus</name>
    <url>http://github.com/josh.marcus/</url>
  </developer>
</developers>

    )
  )

  lazy val tasks:Project = Project("tasks", file("tasks")).
    settings(
      scalaVersion := "2.10.0-RC3",
      libraryDependencies ++= Seq("com.beust" % "jcommander" % "1.23"),
      mainClass in Compile := Some("geotrellis.run.Tasks")
    ).
    dependsOn(root)

  lazy val demo:Project = Project("demo", file("demo")).
    settings(scalaVersion := "2.10.0-RC3").
    dependsOn(root)

  lazy val benchmark:Project = Project("benchmark", file("benchmark")).
    settings(benchmarkSettings: _*).
    dependsOn(root)

  def benchmarkSettings = Seq(
    scalaVersion := "2.10.0-RC3",

    // raise memory limits here if necessary
    //javaOptions in run += "-Xmx8G",
    javaOptions += "-Xmx8G",

    libraryDependencies ++= Seq(
      "com.google.guava" % "guava" % "r09",
      "com.google.code.java-allocation-instrumenter" % "java-allocation-instrumenter" % "2.0",
      "com.google.code.caliper" % "caliper" % "1.0-SNAPSHOT" from "http://plastic-idolatry.com/jars/caliper-1.0-SNAPSHOT.jar",
      "com.google.code.gson" % "gson" % "1.7.1",
      "org.scalatest" % "scalatest_2.10.0-RC3" % "1.8-B1" % "test",
      "junit" % "junit" % "4.5" % "test"
    ),

    // enable forking in both run and test
    fork := true,

    // set the correct working directory for acces to resources in test

    runner in Compile in run <<= (thisProject, taskTemporaryDirectory, scalaInstance, baseDirectory, javaOptions, outputStrategy, javaHome, connectInput) map {
      (tp, tmp, si, base, options, strategy, javaHomeDir, connectIn) =>
      new MyRunner(tp.id, ForkOptions(scalaJars = si.jars,
                                      javaHome = javaHomeDir,
                                      connectInput = connectIn,
                                      outputStrategy = strategy,
                                      runJVMOptions = options,
                                      workingDirectory = Some(base)))
    }
  )
}

class MyRunner(subproject: String, config: ForkScalaRun) extends sbt.ScalaRun {
  def run(mainClass: String, classpath: Seq[File], options: Seq[String], log: Logger): Option[String] = {
    log.info("Running " + subproject + " " + mainClass + " " + options.mkString(" "))

    val javaOptions = classpathOption(classpath) ::: mainClass :: options.toList
    val strategy = config.outputStrategy getOrElse LoggedOutput(log)
    val jvmopts = config.runJVMOptions ++ javaOptions
    //println("jvmopts: %s" format jvmopts)
    val process =  Fork.java.fork(config.javaHome,
                                  jvmopts,
                                  config.workingDirectory,
                                  Map.empty,
                                  config.connectInput,
                                  strategy)
    def cancel() = {
      log.warn("Run canceled.")
      process.destroy()
      1
    }
    val exitCode = try process.exitValue() catch { case e: InterruptedException => cancel() }
    processExitCode(exitCode, "runner")
  }
  private def classpathOption(classpath: Seq[File]) = "-classpath" :: Path.makeString(classpath) :: Nil
  private def processExitCode(exitCode: Int, label: String) = {
    if(exitCode == 0) None
    else Some("Nonzero exit code returned from " + label + ": " + exitCode)
  }
}

// vim: set ts=4 sw=4 et:
