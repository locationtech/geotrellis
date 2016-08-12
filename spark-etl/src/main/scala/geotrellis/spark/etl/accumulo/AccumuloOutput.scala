package geotrellis.spark.etl.accumulo

import geotrellis.spark.etl.OutputPlugin
import geotrellis.spark.etl.config.{AccumuloProfile, BackendProfile, EtlConf}
import geotrellis.spark.io.accumulo.{AccumuloAttributeStore, AccumuloWriteStrategy, HdfsWriteStrategy, SocketWriteStrategy}
import com.typesafe.scalalogging.slf4j.LazyLogging

trait AccumuloOutput[K, V, M] extends OutputPlugin[K, V, M] with LazyLogging {
  val name = "accumulo"

  def strategy(profile: Option[BackendProfile]): AccumuloWriteStrategy = profile match {
    case Some(ap: AccumuloProfile) => {
      val strategy = ap.strategy
        .map {
          _ match {
            case "hdfs" => ap.ingestPath match {
              case Some(ingestPath) =>
                HdfsWriteStrategy(ingestPath)
              case None =>
                AccumuloWriteStrategy.DEFAULT
            }
            case "socket" => SocketWriteStrategy()
          }
        }
        .getOrElse(AccumuloWriteStrategy.DEFAULT)
      logger.info(s"Using Accumulo write strategy: $strategy")
      strategy
    }
    case _ => throw new Exception("Backend profile not matches backend type")
  }
  
  def attributes(conf: EtlConf) = AccumuloAttributeStore(getInstance(conf.outputProfile).connector, getPath(conf.output.backend).table)
}
