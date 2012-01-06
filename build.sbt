import eu.henkelmann.sbt.JUnitXmlTestsListener

name := "Trellis"

organization := "Azavea"

scalaVersion := "2.9.1"

resolvers ++= Seq(
  "Geotools" at "http://download.osgeo.org/webdav/geotools/", 
  "NL4J Repository" at "http://nativelibs4java.sourceforge.net/maven/",
  "maven2 dev repository" at "http://download.java.net/maven/2",
  "Scala Test" at "http://www.scala-tools.org/repo-reloases/",
  "Typesafe Repo" at "http://repo.typesafe.com/typesafe/releases/",
  "Local Maven Repository" at "file://"+Path.userHome.absolutePath+"/.m2/repository",
  "sonatypeSnapshots" at "http://oss.sonatype.org/content/repositories/snapshots"
)

libraryDependencies ++= { 
  val geotoolsVersion = "2.7.4"
  Seq(
    "org.scalatest" %% "scalatest" % "1.6.1",
    "junit" % "junit" % "4.5",
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
    "com.typesafe.akka" % "akka-kernel" % "2.0-M1",
    "com.typesafe.akka" % "akka-remote" % "2.0-M1",
    "com.typesafe.akka" % "akka-actor"  % "2.0-M1",
    "com.google.code.java-allocation-instrumenter" % "java-allocation-instrumenter" % "2.0",
    "com.google.code.caliper" % "caliper" % "1.0-SNAPSHOT",
    "com.google.code.gson" % "gson" % "1.8-SNAPSHOT",
    "net.liftweb" %% "lift-json" % "2.4-M5")
}

scalacOptions += "-deprecation"

scalacOptions += "-unchecked"

scalacOptions += "-optimize"

// currently 1.0-SNAPSHOT breaks scct's test-coverage, but 0.1 doesn't
//val scalacl = compilerPlugin("com.nativelibs4java" % "scalacl-compiler-plugin" % "1.0-SNAPSHOT")
//val scalacl = compilerPlugin("com.nativelibs4java" % "scalacl-compiler-plugin" % "0.1")

//mainClass in (Compile, run) := Some("trellis.run.repl.Main")

testListeners <+= target.map(tgt => new JUnitXmlTestsListener(tgt.toString))

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

// In some cases we've found SBT to hang while running our tests in parallel
parallelExecution := false

// TODO:
//
//  lazy val executable = task { args =>
//      if( args.length == 2)
//        actionConstructor(args(0), args(1))
//      else
//        actionConstructor("trellis.run.repl.Main", "trellis_repl.jar")
//        //task { Some("Usage: example trellis.run.YourClass executable.jar") }
//   } 
//
//  def outputJarPath(artifactName:String):String = (outputPath / artifactName).toString()
//  def outJarsArg (artifactName:String):List[String] = "-outjars" :: outputJarPath(artifactName) :: Nil
//
