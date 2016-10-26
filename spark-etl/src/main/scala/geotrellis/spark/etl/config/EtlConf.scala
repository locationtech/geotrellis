package geotrellis.spark.etl.config

class EtlConf(val input: Input, val output: Output) extends Serializable {
  def outputProfile = output.backend.profile
  def inputProfile  = input.backend.profile
}

object EtlConf extends BaseEtlConf
