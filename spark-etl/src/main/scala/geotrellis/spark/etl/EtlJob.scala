package geotrellis.spark.etl

import geotrellis.spark.etl.config._

case class EtlJob(config: Config, inputCredentials: Option[Backend] = None, outputCredentials: Option[Backend] = None) {
  private def props(params: Map[String, String], credentials: Option[Backend]) = {
    params ++ credentials.collect {
      case credentials: Accumulo => credentials.strategy.fold(Map.empty[String, String])(s => Map("strategy" -> s))
      case credentials: S3       => credentials.partitionsCount.fold(Map.empty[String, String])(c => Map("partitionsCount" -> c.toString))
    }.getOrElse(Map())
  }

  def inputProps: Map[String, String]  = props(config.inputParams, inputCredentials)
  def outputProps: Map[String, String] = props(config.outputParams, outputCredentials)
}
