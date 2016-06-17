package geotrellis.spark.etl

import geotrellis.spark.etl.config._

case class EtlJob(config: Config, inputCredentials: Option[Backend] = None, outputCredentials: Option[Backend] = None) {
  def inputProps: Map[String, String] =
    config.inputParams ++ inputCredentials.collect {
      case credentials: Accumulo => Map("strategy" -> credentials.strategy)
      case credentials: S3       => Map("partitionsCount" -> credentials.partitionsCount.toString)
    }.getOrElse(Map())

  def outputProps: Map[String, String] =
    config.outputParams ++ outputCredentials.collect {
      case credentials: Accumulo => Map("strategy"  -> credentials.strategy)
      case credentials: S3       => Map("partitionsCount" -> credentials.partitionsCount.toString)
    }.getOrElse(Map())
}
