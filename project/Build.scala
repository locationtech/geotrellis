import sbt._
import sbt.Keys._

object MyBuild extends Build {
  val geotoolsVersion = "2.7.4"

  lazy val project = Project("root", file(".")) settings(
    organization := "Azavea",
    name := "Trellis",
    version := "0.6",
    scalaVersion := "2.9.1",

    scalacOptions ++= Seq("-deprecation", "-unchecked", "-optimize"),

    parallelExecution := false,
    testListeners <+= target.map(tgt => new eu.henkelmann.sbt.JUnitXmlTestsListener(tgt.toString)),

    libraryDependencies ++= Seq(
      "org.scalatest" %% "scalatest" % "1.6.1" % "test",
      "junit" % "junit" % "4.5" % "test",
      "com.vividsolutions" % "jts" % "1.8",
      "java3d" % "j3d-core" % "1.3.1",
      "org.geotools" % "gt-main" % geotoolsVersion,
      "org.geotools" % "gt-epsg-hsql" % geotoolsVersion,
      "org.geotools"      % "gt-shapefile" % geotoolsVersion,
      "org.geotools" % "gt-jdbc" % geotoolsVersion,
      "org.geotools.jdbc" % "gt-jdbc-postgis" % geotoolsVersion,
      "org.geotools" % "gt-coverage" % geotoolsVersion,
      "org.geotools" % "gt-coverageio" % geotoolsVersion,
      "org.geotools" % "gt-coveragetools" % geotoolsVersion,
      "org.postgis" % "postgis-jdbc" % "1.3.3",
      "javax.media" % "jai_core" % "1.1.3",
      "postgresql" % "postgresql" % "8.4-701.jdbc4",
      "net.liftweb" %% "lift-json" % "2.4-M5",
      "com.typesafe.akka" % "akka-kernel" % "2.0-M2",
      "com.typesafe.akka" % "akka-remote" % "2.0-M2",
      "com.typesafe.akka" % "akka-actor"  % "2.0-M2",
      "com.google.code.java-allocation-instrumenter" % "java-allocation-instrumenter" % "2.0",
      "com.google.code.caliper" % "caliper" % "1.0-SNAPSHOT" from "http://n0d.es/jars/caliper-1.0-SNAPSHOT.jar",
      "com.google.code.gson" % "gson" % "1.7.1",
      "org.eclipse.jetty" % "jetty-webapp" % "8.1.0.RC4",
      "com.sun.jersey" % "jersey-bundle" % "1.11",
      "com.azavea.math" %% "numeric" % "0.1" from "http://n0d.es/jars/numeric_2.9.1-0.1.jar",
      "com.azavea.math.plugin" %% "optimized-numeric" % "0.2" from "http://plastic-idolatry.com/jars/optimized-numeric-plugin_2.9.1-0.2.jar"
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

    // caliper stuff stolen shamelessly from scala-benchmarking-template

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


object PluginDef extends Build {
  lazy val root = Project("plugins", file(".")) dependsOn(pamflet)

  lazy val pamflet = uri("git://github.com/n8han/pamflet-plugin#0.3.0")
}

// TODO: add proguard stuff back in. this is from old build.sbt file:
/*
seq(ProguardPlugin.proguardSettings :_*)

proguardOptions := Seq(
  "-keepclasseswithmembers public class * { public static void main(java.lang.String[]); }",
  //"-dooptimize",
  //"-dontobfuscate",
  "-dontshrink",
  "-dontoptimize",
  """
    -keepclassmembers class * implements java.io.Serializable {
        static long serialVersionUID;
        private void writeObject(java.io.ObjectOutputStream);
        private void readObject(java.io.ObjectInputStream);
        java.lang.Object writeReplace();
        java.lang.Object readResolve();
    }
  """,
  "-keep class scala.** { *; }",
  "-keep class ch.** { *; }",
  "-keep class trellis.** { *; }",
  "-keep class jline.** { *; }",
  "-keep interface scala.ScalaObject",
  "-keep interface scala.tools.nsc.Interpreter$DebugParam")

makeInJarFilter ~= (j => ((file) => j + ",!**.RSA,!**.SF"))

// May not even need this--- I think scalaLib is included by default!
proguardInJars <+= scalaInstance.map ((_:sbt.ScalaInstance).libraryJar)

*/
