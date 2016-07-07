package geotrellis.spark.etl.config

case class Output(ingestOutputType: IngestOutputType, path: String) {
  def outputParams = getParams(ingestOutputType.output, path)
}
