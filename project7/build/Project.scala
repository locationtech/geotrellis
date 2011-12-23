import sbt._
import reaktor.scct.ScctProject
import eu.henkelmann.sbt.JUnitXmlTestsListener
import proguard.{Configuration=>ProGuardConfiguration, ProGuard, ConfigurationParser}

class TrellisProject(i: ProjectInfo) extends DefaultProject(i) with ScctProject with ProguardProject {

  // SXR stuff
  /*override def compileOptions = {
    CompileOption("-P:sxr:base-directory:" + mainScalaSourcePath.absolutePath) ::
    CompileOption("-P:sxr:output-formats:html+vim") ::
    CompileOption("-optimise") ::
    super.compileOptions.toList
  }

  override def testCompileOptions = {
    CompileOption("-P:sxr:base-directory:" + testScalaSourcePath.absolutePath) ::
    CompileOption("-P:sxr:output-formats:html+vim") ::
    CompileOption("-optimise") ::
    super.compileOptions.toList
  }
*/

  //override def compileOptions = super.compileOptions ++ compileOptions("-unchecked")

  // istanbul repo path
  val trellisRepo = "http://192.168.1.75/repo/trellis"

  // scalatest
  val scalatest = "org.scalatest" % "scalatest" % "3.2" from "%s/scalatest-1.2.jar".format(trellisRepo) 
  val junit = "junit" % "junit" % "4.5" % "test" from "%s/junit-4.5.jar".format(trellisRepo)

  // java topology suite
  val jts = "com.vividsolutions" % "jts" % "1.8" from "http://repo1.maven.org/maven2/com/vividsolutions/jts/1.8/jts-1.8.jar"
  val j3dcore       = "java3d" % "j3d-core" % "1.3.1"

  // scala X-ray
  //val sxr = compilerPlugin("org.scala-tools.sxr" %% "sxr" % "0.2.6")

  // compiler plugin to optimize for loops, etc.
  val nl4jRepo = "NL4J Repository" at "http://nativelibs4java.sourceforge.net/maven/"

  // geotools stuff
  val GEOTOOLS_VERSION = "2.7-M3"
  


  // resolving a dependency for geotools
  val mavenDevRepo = "maven2 dev repository" at "http://download.java.net/maven/2"
  //val javaxMeasureConf  = ModuleConfiguration("javax.measure", mavenDevRepo)

  //OpenGeo modules originally from:
  //val opengeoRepo   = "OSGeo Maven Repository" at "http://download.osgeo.org/webdav/geotools/"
  //val geotoolsConf  = ModuleConfiguration("org.geotools", opengeoRepo)

  val gtMain        = "org.geotools"      % "gt-main" % GEOTOOLS_VERSION from "%s/gt-main-%s.jar".format(trellisRepo, GEOTOOLS_VERSION)
  val gtReferencing = "org.geotools"      % "gt-epsg-hsql" % GEOTOOLS_VERSION from "%s/gt-epsg-hsql-%s.jar".format(trellisRepo, GEOTOOLS_VERSION)
  val gtShapefile   = "org.geotools"      % "gt-shapefile" % GEOTOOLS_VERSION from "%s/gt-shapefile-%s.jar".format(trellisRepo, GEOTOOLS_VERSION)
  //val gtJDBC        = "org.geotools"      % "gt-jdbc" % GEOTOOLS_VERSION from "%s/gt-jdbc-%s.jar".format(trellisRepo, GEOTOOLS_VERSION)
  //val gtDirectory   = "org.geotools"      % "gt-directory" % GEOTOOLS_VERSION 
  //val gtPostgis     = "org.geotools.jdbc" % "gt-jdbc-postgis" % GEOTOOLS_VERSION from "%s/gt-jdbc-postgis%s.jar".format(trellisRepo, GEOTOOLS_VERSION)
  val gtCoverage    = "org.geotools"      % "gt-coverage" % GEOTOOLS_VERSION from "%s/gt-coverage-%s.jar".format(trellisRepo, GEOTOOLS_VERSION)
  val gtCoverageIO = "org.geotools" % "gt-coverageio" % GEOTOOLS_VERSION from "%s/gt-coverageio-%s.jar".format(trellisRepo, GEOTOOLS_VERSION)
  val gtCoverageTools = "org.geotools" % "gt-coveragetools" % GEOTOOLS_VERSION from "%s/gt-coveragetools-%s.jar".format(trellisRepo, GEOTOOLS_VERSION)
  
  // jai_core originally from: ModuleConfiguration("javax.media", opengeoRepo)
  val jaicore = "javax.media" % "jai_core" % "1.1.3" from "http://download.osgeo.org/webdav/geotools/javax/media/jai_core/1.1.3/jai_core-1.1.3.jar"

  val postgresql = "postgresql" % "postgresql" % "8.4-701.jdbc4"
   
  // currently 1.0-SNAPSHOT breaks scct's test-coverage, but 0.1 doesn't
  //val scalacl = compilerPlugin("com.nativelibs4java" % "scalacl-compiler-plugin" % "1.0-SNAPSHOT")
  //val scalacl = compilerPlugin("com.nativelibs4java" % "scalacl-compiler-plugin" % "0.1")

  //create a listener that writes to the normal output directory
  def junitXmlListener: TestReportListener = new JUnitXmlTestsListener(outputPath.toString)
  //add the new listener to the already configured ones
  override def testListeners: Seq[TestReportListener] = super.testListeners ++ Seq(junitXmlListener)


  // for proguard

  //program entry point
  override def mainClass: Option[String] = Some("trellis.run.repl.Main")

  // proguard stuff
  override def proguardOptions = List(
    "-keepclasseswithmembers public class * { public static void main(java.lang.String[]); }",
    //"-dooptimize",
    //"-dontobfuscate",
    "-dontshrink",
    "-dontoptimize",
    proguardKeepLimitedSerializability,
    proguardKeepAllScala,
    "-keep class ch.** { *; }",
    "-keep class trellis.** { *; }",
    "-keep class jline.** { *; }",
    "-keep interface scala.ScalaObject",
    "-keep interface scala.tools.nsc.Interpreter$DebugParam"
  )

  // strip jar signatures to prevent security exceptions
  override def makeInJarFilter (file:String) =
    super.makeInJarFilter(file) + ",!**.RSA,!**.SF"

  override def proguardInJars = Path.fromFile(scalaLibraryJar) +++ super.proguardInJars

  //def fooAction = proguardTask dependsOn(`package`) describedAs("Build a single executable jar with a given Main class")

  lazy val executable = task { args =>
      if( args.length == 2)
        actionConstructor(args(0), args(1))
      else
        actionConstructor("trellis.run.repl.Main", "trellis_repl.jar")
        //task { Some("Usage: example trellis.run.YourClass executable.jar") }
   } 

  def outputJarPath(artifactName:String):String = (outputPath / artifactName).toString()
  def outJarsArg (artifactName:String):List[String] = "-outjars" :: outputJarPath(artifactName) :: Nil


def actionConstructor(mainClass: String, artifactName: String) = task {
    val args = proguardInJarsArg ::: outJarsArg(artifactName) ::: proguardLibJarsArg ::: proguardDefaultArgs
    val config = new ProGuardConfiguration
    println("Proguard args: " + args)
    new ConfigurationParser(args.toArray[String], info.projectPath.asFile).parse(config)
    new ProGuard(config).execute
    None
  }
}
