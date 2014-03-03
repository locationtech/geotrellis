import sbt._

object Dependencies {
  val resolutionRepos = Seq(
    "Local Maven Repository"  at "file://" + Path.userHome.absolutePath + "/.m2/repository",
    "NL4J Repository"         at "http://nativelibs4java.sourceforge.net/maven/",
    "maven2 dev repository"   at "http://download.java.net/maven/2",
    "Typesafe Repo"           at "http://repo.typesafe.com/typesafe/releases/",
    "spray repo"              at "http://repo.spray.io/",
    "sonatypeSnapshots"       at "http://oss.sonatype.org/content/repositories/snapshots"
  )

  val scalatest     = "org.scalatest"       % "scalatest_2.10"  % "2.0.M5b"
  val scalacheck    = "org.scalacheck"      %% "scalacheck"     % "1.11.1"
  val scalaReflect  = "org.scala-lang"      % "scala-reflect"   % "2.10.2"
  val scalaxyLoops  = "com.nativelibs4java" %% "scalaxy-loops"  % "0.3-SNAPSHOT"
  val sigar         = "org.hyperic"         % "sigar"           % "1.6.4"  
  val jts           = "com.vividsolutions"  % "jts"             % "1.13"

  val akkaVersion   = "2.2.3"
  val akkaKernel    = "com.typesafe.akka" %% "akka-kernel"  % akkaVersion
  val akkaRemote    = "com.typesafe.akka" %% "akka-remote"  % akkaVersion
  val akkaActor     = "com.typesafe.akka" %% "akka-actor"   % akkaVersion
  val akkaCluster   = "com.typesafe.akka" %% "akka-cluster" % akkaVersion
  
  val jacksonCore   = "org.codehaus.jackson" % "jackson-core-asl"   % "1.6.1"
  val jacksonMapper = "org.codehaus.jackson" % "jackson-mapper-asl" % "1.6.1"
  
  val spire         = "org.spire-math" %% "spire" % "0.7.1"
  
  val sprayClient   = "io.spray"        % "spray-client"  % "1.2.0"
  val sprayRouting  = "io.spray"        % "spray-routing" % "1.2.0"
  val sprayTestkit  = "io.spray"        % "spray-testkit" % "1.2.0"
  val sprayCan      = "io.spray"        % "spray-can"     % "1.2.0"
  val sprayHttpx    = "io.spray"        % "spray-httpx"   % "1.2.0"
  
  val apacheMath   = "org.apache.commons" % "commons-math3" % "3.2"

  val jettyWebapp   = "org.eclipse.jetty" % "jetty-webapp" % "8.1.0.RC4"
  val jerseyBundle  = "com.sun.jersey"    % "jersey-bundle" % "1.11"
  val slf4jApi      = "org.slf4j"         % "slf4j-api" % "1.6.0"
  val slf4jNop      = "org.slf4j"         % "slf4j-nop" % "1.6.0"
  val asm           = "asm"               % "asm"       % "3.3.1" 
  
  val jcommander    = "com.beust"       % "jcommander"    % "1.23"
  val reflections    = "org.reflections" % "reflections"  % "0.9.5"
  
 
}

