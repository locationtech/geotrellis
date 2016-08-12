package geotrellis.spark.etl.hadoop

import geotrellis.spark.etl.OutputPlugin
import geotrellis.spark.etl.config.EtlConf
import geotrellis.spark.io.hadoop._
import org.apache.spark.SparkConf
import org.apache.spark.deploy.SparkHadoopUtil

trait HadoopOutput[K, V, M] extends OutputPlugin[K, V, M] {
  val name = "hadoop"

  //This should be a safe way to get a hadoop configuration that includes all the environment changes from spark
  def attributes(conf: EtlConf) =
    HadoopAttributeStore(getPath(conf.output.backend).path, SparkHadoopUtil.get.newConfiguration(new SparkConf()))
}
