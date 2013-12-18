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
		
		new SparkContext(sparkMaster, appName, sparkHome, Seq(gtJar))
	}
	
	def createHadoopConfiguration = new Configuration
}