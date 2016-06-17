package geotrellis.spark.etl.config.backend

import geotrellis.spark.etl.config.dataset.Config

case class Credentials(accumulo: List[Accumulo], cassandra: List[Cassandra], s3: List[S3], hadoop: List[Hadoop]) {
  private def getCfgs[T <: Backend](b: List[T]) = b.map(e => e.name -> e).toMap

  def getAccumuloCfgs  = getCfgs(accumulo)
  def getCassandraCfgs = getCfgs(cassandra)
  def getS3Cfgs        = getCfgs(s3)
  def getHadoopCfgs    = getCfgs(hadoop)

  def get(backend: BackendType, credentials: Option[String]): Option[Backend with Product with Serializable] =
    credentials.map((backend match {
      case S3Type                 => getS3Cfgs
      case AccumuloType           => getAccumuloCfgs
      case CassandraType          => getCassandraCfgs
      case HadoopType | FileType  => getHadoopCfgs
    })(_))

  def getOutput(config: Config) = get(config.ingestType.output, config.ingestType.outputCredentials)
  def getInput(config: Config)  = get(config.ingestType.input, config.ingestType.inputCredentials)
}