import Dependencies._

lazy val commonSettings = Seq(
  version := Version.geotrellis,
  scalaVersion := Version.scala,
  description := Info.description,
  crossScalaVersions := Seq("2.11.5", "2.10.4"),
  organization := "com.azavea.geotrellis",
  licenses := Seq("Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0.html")),
  homepage := Some(url(Info.url)),
  scalacOptions ++= Seq(
    "-deprecation",
    "-unchecked",
    "-Yinline-warnings",
    "-language:implicitConversions",
    "-language:reflectiveCalls",
    "-language:higherKinds",
    "-language:postfixOps",
    "-language:existentials",
    "-feature"),
  publishMavenStyle := true,
  publishArtifact in Test := false,
  pomIncludeRepository := { _ => false },
  bintrayPackageLabels := Info.tags,
  bintrayOrganization := Some("azaveaci"),
  bintrayVcsUrl := Some("https://github.com/geotrellis/geotrellis.git"),
  pomExtra := (
    <scm>
      <url>git@github.com:geotrellis/geotrellis.git</url>
      <connection>scm:git:git@github.com:geotrellis/geotrellis.git</connection>
      </scm>
      <developers>
      <developer>
      <id>echeipesh</id>
      <name>Eugene Cheipesh</name>
      <url>http://github.com/echeipesh/</url>
        </developer>
      <developer>
      <id>lossyrob</id>
      <name>Rob Emanuele</name>
      <url>http://github.com/lossyrob/</url>
        </developer>
      </developers>),
  shellPrompt := { s => Project.extract(s).currentProject.id + " > " }
)

lazy val root = Project("geotrellis", file(".")).
  dependsOn(raster, vector, proj4).
  settings(
    initialCommands in console :=
      """
      import geotrellis.raster._
      import geotrellis.vector._
      import geotrellis.proj4._
      """
  )

lazy val macros = Project("macros", file("macros")).
  settings(commonSettings: _*).
  settings(
    name := "geotrellis-macros",
    libraryDependencies <++= scalaVersion {
      case "2.10.4" => Seq(
        "org.scala-lang" %  "scala-reflect" % "2.10.4",
        "org.scalamacros" %% "quasiquotes" % "2.0.1",
        "org.spire-math" %% "spire-macros" % "0.9.1"
      )
      case "2.11.5" => Seq(
        "org.scala-lang" %  "scala-reflect" % "2.11.5",
        "org.spire-math" %% "spire-macros" % "0.9.1"
      )
    },
    resolvers += Resolver.sonatypeRepo("snapshots"),
    addCompilerPlugin("org.scalamacros" % "paradise" % "2.0.1" cross CrossVersion.full)
  )

lazy val vector = Project("vector", file("vector")).
  dependsOn(proj4).
  settings(commonSettings: _*).
  settings(
    name := "geotrellis-vector",
    libraryDependencies ++= Seq(
      jts,
      sprayJson,
      apacheMath,
      spire)
  )

lazy val vectorTest = Project("vector-test", file("vector-test")).
    dependsOn(vector, testkit).
    settings(
      name := "geotrellis-vector-test",
      libraryDependencies ++= Seq(
        scalatest   % "test",
        scalacheck  % "test")
    )

lazy val proj4 = Project("proj4", file("proj4")).
  settings(commonSettings: _*).
  settings(
    name := "geotrellis-proj4",
    libraryDependencies ++= Seq(
      openCSV,
      "org.parboiled" %% "parboiled" % "2.0.0" % "test",
      scalatest   % "test",
      scalacheck  % "test")      
  )


lazy val raster = Project("raster", file("raster")).
  dependsOn(macros, vector).    
  settings(commonSettings: _*).
  settings(
    name := "geotrellis-raster",
    parallelExecution := false,
    fork in test := false,
    javaOptions in run += "-Xmx2G",
    scalacOptions ++= Seq(
      "-optimize",
      "-language:experimental.macros"),
    libraryDependencies ++= Seq(
      typesafeConfig,
      jts,
      spire,
      monocleCore,
      monocleMacro,
      openCSV),
    addCompilerPlugin("org.scalamacros" % "paradise" % "2.0.1" cross CrossVersion.full)
  )

lazy val rasterTest = Project("raster-test", file("raster-test")).
    dependsOn(raster, testkit).
    settings(commonSettings: _*).
    settings(
      name := "geotrellis-raster-test",      
      parallelExecution := false,
      fork in test := false,
      javaOptions in run += "-Xmx2G",
      scalacOptions in compile += "-optimize",
      libraryDependencies ++= Seq(
        scalatest % "test",
        scalacheck  % "test",
        spire % "test",
        sprayClient % "test",
        sprayRouting % "test"),
      addCompilerPlugin("org.scalamacros" % "paradise" % "2.0.1" cross CrossVersion.full)
    ) 

lazy val engine = Project("engine", file("engine")).
  dependsOn(raster).
  settings(commonSettings: _*).
  settings(
    name := "geotrellis-engine",
    scalacOptions in compile += "-optimize",
    libraryDependencies ++= Seq(
      akkaKernel, akkaRemote, akkaActor, akkaCluster,
      spire, monocleCore, monocleMacro, sprayClient // for reading args from URLs,
    )
  )

lazy val engineTest = Project("engine-test", file("engine-test")).
  dependsOn(engine, testkit).
  settings(commonSettings: _*).
  settings(
    name := "geotrellis-engine-test",
    parallelExecution := true,
    fork in test := true,
    javaOptions in run += "-Xmx4G",
    scalacOptions in compile += "-optimize",
    libraryDependencies ++= Seq(
      sprayClient % "test",
      sprayRouting % "test",
      scalatest % "test")
  )


lazy val testkit = Project("testkit", file("testkit")).
  dependsOn(raster, engine).
  settings(commonSettings: _*).
  settings(
    name := "geotrellis-testkit",
    libraryDependencies += scalatest
  )

lazy val services = Project("services", file("services")).
  dependsOn(raster, vector, engine).
  settings(commonSettings: _*).
  settings(name := "geotrellis-services")

lazy val jetty = Project("jetty", file("jetty")).
  dependsOn(services).
  settings(commonSettings: _*).
  settings(    
    name := "geotrellis-jetty",
    libraryDependencies ++= Seq(
      jettyWebapp,
      jerseyBundle,
      slf4jApi,
      asm)
  )

lazy val geotrellisSlick = Project("slick", file("slick")).
  dependsOn(vector).
  settings(commonSettings: _*).
  settings(
    name := "geotrellis-slick",
    libraryDependencies := Seq(
      slick,
      postgresql,
      slf4jApi,
      scalatest % "test")
  )

lazy val examples = Project("examples", file("examples")).
  dependsOn(raster, vector).
  settings(commonSettings: _*).
  settings(
    name := "geotrellis-examples",
    fork := true
  ) 

lazy val admin = Project("admin", file("admin")).
  dependsOn(raster, services, vector).
  settings(commonSettings: _*).
  settings(
    name := "geotrellis-admin",
    fork := true,
    libraryDependencies ++= Seq(
      scalatest % "test",
      sprayTestkit % "test",
      sprayRouting,
      sprayCan,
      sprayHttpx)
  )

lazy val spark = Project("spark", file("spark")).
  dependsOn(raster, gdal).
  settings(commonSettings: _*).
  settings(
    name := "geotrellis-spark",
    fork := true,
    parallelExecution in Test := false,
    javaOptions ++= List(
      "-Xmx8G",
      s"-Djava.library.path=${Environment.javaGdalDir}",
      "-Dsun.io.serialization.extendedDebugInfo=true"),
    libraryDependencies ++= Seq(
      "org.apache.accumulo" % "accumulo-core" % Version.accumulo 
        exclude("org.jboss.netty", "netty")
        exclude("org.apache.hadoop", "hadoop-client"),
      "org.apache.spark" %% "spark-core" % Version.spark % "provided",
      "org.apache.hadoop" % "hadoop-client" % Version.hadoop % "provided",
      "de.javakaffee" % "kryo-serializers" % "0.27",
      "com.google.uzaygezen" % "uzaygezen-core" % "0.2",
      logging, awsSdkS3, avro,
      spire,
      monocleCore, monocleMacro,
      nscalaTime,
      scalazStream,
      scalatest % "test"),
    initialCommands in console :=
      """
      import geotrellis.raster._
      import geotrellis.vector._
      import geotrellis.proj4._
      import geotrellis.spark._
      import geotrellis.spark.utils._
      import geotrellis.spark.tiling._
      """
  )

lazy val sparkEtl = Project(id = "spark-etl", base = file("spark-etl")).
  dependsOn(spark).
  settings(commonSettings: _*).
  settings(
    name := "geotrellis-spark-etl",
    libraryDependencies ++= Seq(
      "com.google.inject" % "guice" % "3.0",
      "com.google.inject.extensions" % "guice-multibindings" % "3.0",
      "org.rogach" %% "scallop" % "0.9.5",
      logging,
      sparkCore % "provided")
  )

lazy val graph = Project("graph", file("graph")).
  dependsOn(spark % "test->test;compile->compile").
  settings(commonSettings: _*).
  settings(
    name := "geotrellis-graph",
    fork := true,
    parallelExecution in Test := false,
    javaOptions ++= List(
      "-Xmx8G",
      s"-Djava.library.path=${Environment.javaGdalDir}",
      "-Dsun.io.serialization.extendedDebugInfo=true"
    ),
    libraryDependencies ++= Seq(
      "org.apache.spark" %% "spark-graphx" % Version.spark % "provided",
      scalatest % "test")
  )


lazy val index = Project("index", file("index")).
  settings(commonSettings: _*).
  settings(
    name := "geotrellis-index",
    libraryDependencies += scalatest % "test"
  )

lazy val gdal: Project = Project("gdal", file("gdal")).
  dependsOn(raster, geotools % "test").
  settings(commonSettings: _*).
  settings(
    name := "geotrellis-gdal",
    javaOptions += s"-Djava.library.path=${Environment.javaGdalDir}",
    libraryDependencies ++= Seq(
      "org.gdal"         % "gdal"       % "1.10.1",
      "com.github.scopt" %% "scopt" % "3.3.0",
      scalatest % "test"),
    resolvers += "OpenGeo" at "http://repo.boundlessgeo.com/main",
    fork in test := true
  )

lazy val geotools = Project("geotools", file("geotools")).
  dependsOn(raster, engine, testkit % "test").
  settings(commonSettings: _*).
  settings(
    name := "geotrellis-geotools",
    libraryDependencies ++= Seq(
      "java3d" % "j3d-core" % "1.3.1",
      "org.geotools" % "gt-main" % Version.geotools,
      "org.geotools" % "gt-coverage" % Version.geotools,
      "org.geotools" % "gt-shapefile" % Version.geotools,
      "org.geotools" % "gt-geotiff" % Version.geotools,
      "org.geotools" % "gt-epsg-hsql" % Version.geotools,
      "javax.media" % "jai_core" % "1.1.3" from "http://download.osgeo.org/webdav/geotools/javax/media/jai_core/1.1.3/jai_core-1.1.3.jar"),
    resolvers += "Geotools" at "http://download.osgeo.org/webdav/geotools/",
    fork in test := false
  ) 

lazy val dev = Project("dev", file("dev")).
  dependsOn(raster, engine).
  settings(commonSettings: _*).
  settings(    
    libraryDependencies += sigar,
    Keys.fork in run := true,
    fork := true,
    javaOptions in run += "-Djava.library.path=./sigar"    
  ) 

lazy val demo = Project("demo", file("demo")).
  dependsOn(jetty).
  settings(commonSettings: _*)

val vectorBenchmarkKey = AttributeKey[Boolean]("vectorJavaOptionsPatched")
val benchmarkKey = AttributeKey[Boolean]("javaOptionsPatched")

lazy val vectorBenchmark: Project = Project("vector-benchmark", file("vector-benchmark")).
  dependsOn(vectorTest % "compile->test").
  settings(commonSettings: _*).
  settings(
    name := "geotrellis-vector-benchmark",
    libraryDependencies ++= Seq(
      scalatest % "test",
      scalacheck % "test",
      caliper,
      "com.google.guava" % "guava" % "r09",
      "com.google.code.java-allocation-instrumenter" % "java-allocation-instrumenter" % "2.0",
      "com.google.code.gson" % "gson" % "1.7.1"),
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
        state.get(vectorBenchmarkKey) match {
          case None =>
            // get the runtime classpath, turn into a colon-delimited string
            Project
              .runTask(fullClasspath in Runtime in vectorBenchmark, state)
              .get
              ._2
              .toEither match {
              case Right(x) =>
                val classPath =
                  x.files
                    .mkString(":")
                // return a state with javaOptionsPatched = true and javaOptions set correctly
                Project
                  .extract(state)
                  .append(
                  Seq(javaOptions in (benchmark, run) ++= Seq("-Xmx8G", "-cp", classPath)),
                    state.put(vectorBenchmarkKey, true)
                )
              case _ => state
            }

          case Some(_) =>
            state // the javaOptions are already patched
        }
      }
    }
  )

lazy val benchmark: Project = Project("benchmark", file("benchmark")).
  dependsOn(raster, engine, geotools, jetty).
  settings(commonSettings: _*).
  settings(
    javaOptions += "-Xmx8G",
    libraryDependencies ++= Seq(
      spire,
      caliper,
      "com.google.guava" % "guava" % "r09",
      "com.google.code.java-allocation-instrumenter" % "java-allocation-instrumenter" % "2.0",
      "com.google.code.gson" % "gson" % "1.7.1"),    
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
        state.get(benchmarkKey) match {
          case None =>
            // get the runtime classpath, turn into a colon-delimited string
            Project
              .runTask(fullClasspath in Runtime in benchmark, state)
              .get
              ._2
              .toEither match {
              case Right(x) =>
                val classPath =
                  x.files
                    .mkString(":")
                // return a state with javaOptionsPatched = true and javaOptions set correctly
                Project
                  .extract(state)
                  .append(
                  Seq(javaOptions in (benchmark, run) ++= Seq("-Xmx8G", "-cp", classPath)),
                    state.put(benchmarkKey, true)
                )
              case _ => state
            }
          case Some(_) =>
            state // the javaOptions are already patched
        }
      }
    }
  ) 