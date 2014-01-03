package geotrellis.spark.utils

import org.apache.spark.SparkContext
import org.apache.hadoop.conf.Configuration

object GeotrellisSparkUtils {
  def createSparkContext(sparkMaster: String, appName: String, gtJarSuffix: String) = {
    val sparkHome = scala.util.Properties.envOrNone("SPARK_HOME") match {
      case Some(value) => value
      case None => throw new Error("Oops, SPARK_HOME is not defined")
    }

    val gtHome = scala.util.Properties.envOrNone("GEOTRELLIS_HOME") match {
      case Some(value) => value
      case None => throw new Error("Oops, GEOTRELLIS_HOME is not defined")
    }

    val gtJar = gtHome + gtJarSuffix

    System.setProperty("spark.serializer", "org.apache.spark.serializer.KryoSerializer")
    System.setProperty("spark.kryo.registrator", "geotrellis.spark.KryoRegistrator")
    
    new SparkContext(sparkMaster, appName, sparkHome, Seq(gtJar))
  }

  def createHadoopConfiguration = {
    // TODO - figure out how to get the conf directory automatically added to classpath via sbt
    // - right now it is manually added
    Configuration.addDefaultResource("core-site.xml")
    Configuration.addDefaultResource("mapred-site.xml")
    Configuration.addDefaultResource("hdfs-site.xml")
    new Configuration
  }
}
