package geotrellis.spark.etl.file

import geotrellis.spark.etl.OutputPlugin
import geotrellis.spark.etl.config.EtlConf
import geotrellis.spark.io.file._

trait FileOutput[K, V, M] extends OutputPlugin[K, V, M] {
  val name = "file"

  //This should be a safe way to get a hadoop configuration that includes all the environment changes from spark
  def attributes(conf: EtlConf) = FileAttributeStore(getPath(conf.output.backend).path)
}
