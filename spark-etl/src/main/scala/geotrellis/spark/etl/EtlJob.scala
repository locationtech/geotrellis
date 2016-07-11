package geotrellis.spark.etl

import geotrellis.spark.etl.config._

case class EtlJob(conf: EtlConf) {
  private def props(params: Map[String, String], profile: Option[BackendProfile]) = {
    params ++ profile.collect {
      case p: AccumuloProfile => p.strategy.fold(Map.empty[String, String])(s => Map("strategy" -> s))
      case p: S3Profile       => p.partitionsCount.fold(Map.empty[String, String])(c => Map("partitionsCount" -> c.toString))
    }.getOrElse(Map())
  }

  def inputProps: Map[String, String]  = props(conf.input.params, conf.inputProfile)
  def outputProps: Map[String, String] = props(conf.output.params, conf.outputProfile)
}
