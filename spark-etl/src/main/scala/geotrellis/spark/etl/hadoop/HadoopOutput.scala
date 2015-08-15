package geotrellis.spark.etl.hadoop

import geotrellis.spark.etl.OutputPlugin
import geotrellis.spark.io.hadoop.HadoopAttributeStore
import org.apache.spark.SparkConf
import org.apache.spark.deploy.SparkHadoopUtil
import org.apache.hadoop.fs.Path

trait HadoopOutput extends OutputPlugin {
  val name = "hadoop"
  val requiredKeys = Array("path")

  //This should be a safe way to get a hadoop configuration that includes all the environment changes from spark
  def attributes(props: Map[String, String]) =
    new HadoopAttributeStore(SparkHadoopUtil.get.newConfiguration(new SparkConf()), new Path(props("path"), "attributes"))
}
