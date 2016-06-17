package geotrellis.spark.etl

import geotrellis.spark.etl.config.backend._
import geotrellis.spark.etl.config.dataset.Config

case class EtlJob(inputCredentials: Option[Backend], outputCredentials: Option[Backend], config: Config) {
  def getInputProps: Map[String, String] = config.getInputParams
  def getOutputProps: Map[String, String] =
    config.getOutputParams ++ outputCredentials.collect { case credentials: Accumulo =>
      Map("strategy"  -> credentials.strategy)
    }.getOrElse(Map())
}
