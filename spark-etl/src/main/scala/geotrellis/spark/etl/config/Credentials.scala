package geotrellis.spark.etl.config

case class Credentials(accumulo: List[Accumulo], cassandra: List[Cassandra], s3: List[S3], hadoop: List[Hadoop]) {
  private def getCfgs[T <: Backend](b: List[T]) = b.map(e => e.name -> e).toMap

  def getAccumuloCfgs: Map[String, Accumulo] = getCfgs(accumulo)
  def getCassandraCfgs = getCfgs(cassandra)
  def getS3Cfgs        = getCfgs(s3)
  def getHadoopCfgs    = getCfgs(hadoop)

  def get(backend: BackendType, credentials: Option[String]): Option[Backend with Product with Serializable] =
    credentials.map((backend match {
      case S3Type                    => getS3Cfgs
      case AccumuloType              => getAccumuloCfgs
      case CassandraType             => getCassandraCfgs
      case HadoopType | FileType     => getHadoopCfgs
      // we are not able to define credentials for custom InputPlugins,
      // that logic should be incapsulated into InputPlugin definition
      case UserDefinedBackendType(s) => Map[String, Backend with Product with Serializable]()
    })(_))

  def getOutput(config: Config) = get(config.ingestType.output, config.ingestType.outputCredentials)
  def getInput(config: Config)  = get(config.ingestType.input, config.ingestType.inputCredentials)
}