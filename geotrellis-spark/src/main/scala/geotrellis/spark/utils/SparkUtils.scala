package geotrellis.spark.utils
import org.apache.hadoop.conf.Configuration
import org.apache.spark.Logging

import org.apache.spark.SparkContext

import java.io.File

object SparkUtils extends Logging {
  def createSparkContext(sparkMaster: String, appName: String) = {
    val sparkHome = scala.util.Properties.envOrNone("SPARK_HOME") match {
      case Some(value) => value
      case None        => throw new Error("Oops, SPARK_HOME is not defined")
    }

    val gtHome = scala.util.Properties.envOrNone("GEOTRELLIS_HOME") match {
      case Some(value) => value
      case None        => throw new Error("Oops, GEOTRELLIS_HOME is not defined")
    }
   
    System.setProperty("spark.serializer", "org.apache.spark.serializer.KryoSerializer")
    System.setProperty("spark.kryo.registrator", "geotrellis.spark.KryoRegistrator")

    new SparkContext(sparkMaster, appName, sparkHome, Seq(jar(gtHome)))
  }

  def createHadoopConfiguration = {
    // TODO - figure out how to get the conf directory automatically added to classpath via sbt
    // - right now it is manually added
    Configuration.addDefaultResource("core-site.xml")
    Configuration.addDefaultResource("mapred-site.xml")
    Configuration.addDefaultResource("hdfs-site.xml")
    new Configuration
  }

  def jar(gtHome: String): String = {
    def isMatch(fileName: String): Boolean = "geotrellis-spark(.)*.jar".r.findFirstIn(fileName) match {
      case Some(_) => true
      case None    => false
    }

    def findJar(file: File): Array[Option[File]] = {
      if (isMatch(file.getName))
        Array(Some(file))
      else if (file.isDirectory())
        file.listFiles().flatMap(findJar(_))
      else Array(None)
    }

    val matches = findJar(new File(gtHome)).flatten
    if (matches.length == 1) {
      logInfo(s"Found unique match for geotrellis-spark jar: ")
      logInfo(matches.mkString("\n"))
      matches(0).getAbsolutePath
    } else if (matches.length > 1) {
      logInfo(s"Found ${matches.length} matches for geotrellis-spark jar: ")
      logInfo(matches.mkString("\n"))
      val firstMatch = matches(0).getAbsolutePath
      logInfo("Using first match: " + firstMatch)
      firstMatch
    } else {
      sys.error("Couldn't find geotrellis jar")
    }
  }
}
