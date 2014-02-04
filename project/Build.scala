import sbt._
import sbt.Keys._

// sbt-assembly
import sbtassembly.Plugin._
import AssemblyKeys._

// ls.implicit.ly
import ls.Plugin.LsKeys
import ls.Plugin.lsSettings

object Version {
  val geotrellis = "0.10.0-SNAPSHOT"
  val scala = "2.10.3"
  val akka = "2.2.3"
}

object Info {
  val description = 
    "GeoTrellis is an open source geographic data processing engine for high performance applications."
  val url = "http://geotrellis.github.io"
  val tags = Seq("maps", "gis", "geographic", "data", "raster", "processing")
}

object GeotrellisBuild extends Build {
  val key = AttributeKey[Boolean]("javaOptionsPatched")

  // Default settings
  override lazy val settings = 
    super.settings ++
    Seq(
      version := Version.geotrellis,
      scalaVersion := Version.scala,
      organization := "com.azavea.geotrellis",

      // disable annoying warnings about 2.10.x
      conflictWarning in ThisBuild := ConflictWarning.disable,
      scalacOptions ++=
        Seq("-deprecation",
          "-unchecked",
          "-Yinline-warnings",
          "-language:implicitConversions",
          "-language:postfixOps",
          "-language:existentials",
          "-feature"),

      publishMavenStyle := true,

      publishTo <<= version { (v: String) =>
        val nexus = "https://oss.sonatype.org/"
        if (v.trim.endsWith("SNAPSHOT"))
          Some("snapshots" at nexus + "content/repositories/snapshots")
        else
          Some("releases" at nexus + "service/local/staging/deploy/maven2")
      },

      publishArtifact in Test := false,

      pomIncludeRepository := { _ => false },
      licenses := Seq("Apache 2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0.html")),
      homepage := Some(url(Info.url)),

      pomExtra := (

        <scm>
          <url>git@github.com:geotrellis/geotrellis.git</url>
          <connection>scm:git:git@github.com:geotrellis/geotrellis.git</connection>
        </scm>
        <developers>
          <developer>
            <id>joshmarcus</id>
            <name>Josh Marcus</name>
            <url>http://github.com/joshmarcus/</url>
          </developer>
          <developer>
            <id>lossyrob</id>
            <name>Rob Emanuele</name>
            <url>http://github.com/lossyrob/</url>
          </developer>
        </developers>)
    )

  val defaultAssemblySettings = 
    assemblySettings ++
    Seq(
      mergeStrategy in assembly <<= (mergeStrategy in assembly) {
        (old) => {
          case "reference.conf" => MergeStrategy.concat
          case "application.conf" => MergeStrategy.concat
          case "META-INF/MANIFEST.MF" => MergeStrategy.discard
          case "META-INF\\MANIFEST.MF" => MergeStrategy.discard
          case _ => MergeStrategy.first
        }
      }
    )

  // Project: macros

  lazy val macros =
    Project("macros", file("macros"))
      .settings(macrosSettings: _*)

  lazy val macrosSettings =
    Seq(
      name := "geotrellis-macros",
      addCompilerPlugin("org.scala-lang.plugins" % "macro-paradise_2.10.2" % "2.0.0-SNAPSHOT"),
      libraryDependencies ++= Seq(
        "org.scala-lang" % "scala-reflect" % "2.10.2"),
      resolvers += Resolver.sonatypeRepo("snapshots"))

  // Project: root

  lazy val root =
    Project("root", file("."))
      .settings(rootSettings: _*)
      .dependsOn(macros)

  lazy val rootSettings =
    Seq(
      name := "geotrellis",
      parallelExecution := false,
      fork in test := false,
      javaOptions in run += "-Xmx2G",
      scalacOptions in compile ++=
        Seq("-optimize"),
      libraryDependencies ++= Seq(
        "org.scalatest" % "scalatest_2.10" % "2.0.M5b" % "test",
        "org.scala-lang" % "scala-reflect" % "2.10.2",
        "com.vividsolutions" % "jts" % "1.12",
        "com.typesafe.akka" %% "akka-kernel" % Version.akka,
        "com.typesafe.akka" %% "akka-remote" % Version.akka,
        "com.typesafe.akka" %% "akka-actor" % Version.akka,
        "com.typesafe.akka" %% "akka-cluster" % Version.akka,
        "org.codehaus.jackson" % "jackson-core-asl" % "1.6.1",
        "org.codehaus.jackson" % "jackson-mapper-asl" % "1.6.1",
        "org.spire-math" %% "spire" % "0.4.0",
        "com.nativelibs4java" %% "scalaxy-loops" % "0.3-SNAPSHOT" % "provided",
        "io.spray"       % "spray-client" % "1.2.0", // for reading args from URLs,
        "io.spray"       % "spray-routing" % "1.2.0" % "test",
        "org.apache.commons" % "commons-math3" % "3.2"
      ),

      resolvers ++= Seq(
        "NL4J Repository" at "http://nativelibs4java.sourceforge.net/maven/",
        "maven2 dev repository" at "http://download.java.net/maven/2",
        "Scala Test" at "http://www.scala-tools.org/repo-reloases/",
        "Typesafe Repo" at "http://repo.typesafe.com/typesafe/releases/",
        "Local Maven Repository" at "file://" + Path.userHome.absolutePath + "/.m2/repository",
        "spray repo" at "http://repo.spray.io/",
        "sonatypeSnapshots" at "http://oss.sonatype.org/content/repositories/snapshots"
      )
    ) ++
    defaultAssemblySettings
    lsSettings ++
    Seq(
      (LsKeys.tags in LsKeys.lsync) :=
        Info.tags,
      (LsKeys.docsUrl in LsKeys.lsync) := 
        Some(new URL(Info.url)),
      (description in LsKeys.lsync) := 
        Info.description
    )


  // Project: services

  lazy val services: Project =
    Project("services", file("services"))
      .settings(servicesSettings: _*)
      .dependsOn(root)

  lazy val servicesSettings =
    Seq(
      name := "geotrellis-services"
    )

  // Project: jetty

  lazy val jetty: Project =
    Project("jetty", file("jetty"))
      .settings(jettySettings: _*)
      .dependsOn(root,services)

  lazy val jettySettings =
    Seq(
      name := "geotrellis-jetty",
      libraryDependencies ++= Seq(
        "org.eclipse.jetty" % "jetty-webapp" % "8.1.0.RC4",
        "com.sun.jersey" % "jersey-bundle" % "1.11",
        "org.slf4j" % "slf4j-api" % "1.6.0",
        "org.slf4j" % "slf4j-nop" % "1.6.0",
        "asm" % "asm" % "3.3.1" )
    ) ++
    defaultAssemblySettings


  // Project: admin

  lazy val admin: Project =
    Project("admin", file("admin"))
      .settings(adminSettings: _*)
      .dependsOn(root,services)

  lazy val adminSettings =
    Seq(
      name := "geotrellis-admin",
      libraryDependencies ++= Seq(
        "io.spray" % "spray-routing" % "1.2-RC4",
        "io.spray" % "spray-can" % "1.2-RC4"
      ),
      resolvers ++= Seq(
        "spray repo" at "http://repo.spray.io"
      )
    ) ++ 
    spray.revolver.RevolverPlugin.Revolver.settings ++
    defaultAssemblySettings

  // Project: spark

  lazy val spark: Project =
    Project("spark", file("geotrellis-spark"))
      .settings(sparkSettings: _*)
      .dependsOn(root)
      .dependsOn(geotools)

  lazy val sparkSettings =
    Seq(
      name := "geotrellis-spark",
      libraryDependencies ++= 
        Seq(
          // first two are just to quell the UnsupportedOperationException in Hadoop's Configuration
          // http://itellity.wordpress.com/2013/05/27/xerces-parse-error-with-hadoop-or-solr-feature-httpapache-orgxmlfeaturesxinclude-is-not-recognized/
          "xerces" % "xercesImpl" % "2.9.1",
          "xalan" % "xalan" % "2.7.1",
          "org.scalatest" % "scalatest_2.10" % "2.0.M5b" % "test",
          "org.apache.spark" %% "spark-core" % "0.9.0-incubating-SNAPSHOT",
          "org.apache.hadoop" % "hadoop-client" % "0.20.2-cdh3u4",
          "com.fasterxml.jackson.module" %% "jackson-module-scala" % "2.3.0",
          "com.nativelibs4java" %% "scalaxy-loops" % "0.3-SNAPSHOT" % "provided",
	      "com.quantifind" %% "sumac" % "0.2.3",
	      "commons-io" % "commons-io" % "2.4"
        ),
      resolvers ++= Seq(
        "Cloudera Repo" at "https://repository.cloudera.com/artifactory/cloudera-repos",
        "sonatypeSnapshots" at "http://oss.sonatype.org/content/repositories/snapshots")
    ) ++ 
    defaultAssemblySettings ++ 
    net.virtualvoid.sbt.graph.Plugin.graphSettings

  // Project: geotools

  val geotoolsVersion = "8.0-M4"
  lazy val geotools: Project =
    Project("geotools", file("geotools"))
      .settings(geotoolsSettings: _*)
      .dependsOn(root % "test->test;compile->compile")

  lazy val geotoolsSettings =
    Seq(
      name := "geotrellis-geotools",
      libraryDependencies ++= 
        Seq(
          "org.scalatest" % "scalatest_2.10" % "2.0.M5b" % "test",
          "java3d" % "j3d-core" % "1.3.1",
          "org.geotools" % "gt-main" % geotoolsVersion,
          "org.geotools" % "gt-jdbc" % geotoolsVersion,
          "org.geotools.jdbc" % "gt-jdbc-postgis" % geotoolsVersion,
          "org.geotools" % "gt-coverage" % geotoolsVersion,
          "org.geotools" % "gt-coveragetools" % geotoolsVersion,
          "org.postgis" % "postgis-jdbc" % "1.3.3",
          "javax.media" % "jai_core" % "1.1.3" from "http://download.osgeo.org/webdav/geotools/javax/media/jai_core/1.1.3/jai_core-1.1.3.jar"
        ),
      resolvers ++= 
        Seq(
          "Geotools" at "http://download.osgeo.org/webdav/geotools/"
        )
    ) ++
    defaultAssemblySettings

  // Project: dev

  lazy val dev: Project =
    Project("dev", file("dev"))
      .settings(devSettings: _*)
      .dependsOn(root)

  lazy val devSettings =
    Seq(
      libraryDependencies ++= 
        Seq(
          "org.scala-lang" % "scala-reflect" % "2.10.2",
          "org.hyperic" % "sigar" % "1.6.4"
        ),
      resolvers ++= 
        Seq(
          "Typesafe Repo" at "http://repo.typesafe.com/typesafe/releases/"
        ),
      Keys.fork in run := true,
      fork := true,
      javaOptions in run ++= 
        Seq(
          "-Djava.library.path=./sigar"
        )
    ) ++
    defaultAssemblySettings

  // Project: demo

  lazy val demo: Project =
    Project("demo", file("demo"))
      .dependsOn(jetty)

  // Project: tasks

  lazy val tasks: Project =
    Project("tasks", file("tasks"))
      .settings(tasksSettings: _*)
      .dependsOn(root, geotools)

  lazy val tasksSettings =
    Seq(
      libraryDependencies ++= 
        Seq(
          "com.beust" % "jcommander" % "1.23",
          "org.reflections" % "reflections" % "0.9.5"
        ),
      libraryDependencies <+= 
        (sbtVersion) { v =>
          v.split('.').toList match {
            case "0" :: "11" :: "3" :: Nil  =>
              "org.scala-sbt" %%
              "launcher-interface" %
              v % "provided"
            case _ =>
              "org.scala-sbt" %
              "launcher-interface" %
              v % "provided"
          }
        },
      mainClass in Compile := Some("geotrellis.run.Tasks")
    ) ++
    defaultAssemblySettings

  // Project: benchmark

  lazy val benchmark: Project =
    Project("benchmark", file("benchmark"))
      .settings(benchmarkSettings: _*)
      .dependsOn(root,geotools)

  def benchmarkSettings =
    Seq(
      // raise memory limits here if necessary
      javaOptions += "-Xmx8G",

      libraryDependencies ++= Seq(
        "com.google.guava" % "guava" % "r09",
        "com.google.code.java-allocation-instrumenter" % "java-allocation-instrumenter" % "2.0",
        "com.google.code.caliper" % "caliper" % "1.0-SNAPSHOT"
          from "http://plastic-idolatry.com/jars/caliper-1.0-SNAPSHOT.jar",
        "com.google.code.gson" % "gson" % "1.7.1",
        "org.spire-math" %% "spire" % "0.7.1",
        "com.nativelibs4java" %% "scalaxy-loops" % "0.3-SNAPSHOT" % "provided"
      ),
      resolvers ++= Seq(
        "NL4J Repository" at "http://nativelibs4java.sourceforge.net/maven/",
        "maven2 dev repository" at "http://download.java.net/maven/2",
        "Scala Test" at "http://www.scala-tools.org/repo-reloases/",
        "Typesafe Repo" at "http://repo.typesafe.com/typesafe/releases/",
        "Local Maven Repository" at "file://" + Path.userHome.absolutePath + "/.m2/repository",
        "sonatypeSnapshots" at "http://oss.sonatype.org/content/repositories/snapshots"
      ),

      // enable forking in both run and test
      fork := true,

      // custom kludge to get caliper to see the right classpath

      // we need to add the runtime classpath as a "-cp" argument to the
      // `javaOptions in run`, otherwise caliper will not see the right classpath
      // and die with a ConfigurationException unfortunately `javaOptions` is a
      // SettingsKey and `fullClasspath in Runtime` is a TaskKey, so we need to
      // jump through these hoops here in order to feed the result of the latter
      // into the former
      onLoad in Global ~= { previous => state =>
        previous {
          state.get(key) match {
            case None =>
              // get the runtime classpath, turn into a colon-delimited string
              val classPath = Project.runTask(fullClasspath in Runtime in benchmark, state).get._2.toEither.right.get.files.mkString(":")
              // return a state with javaOptionsPatched = true and javaOptions set correctly
              Project.extract(state).append(Seq(javaOptions in (benchmark, run) ++= Seq("-Xmx8G", "-cp", classPath)), state.put(key, true))
            case Some(_) =>
              state // the javaOptions are already patched
          }
        }
      }
    ) ++
  defaultAssemblySettings
}
