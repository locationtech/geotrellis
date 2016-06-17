package geotrellis.spark.etl.accumulo

import geotrellis.spark.etl.OutputPlugin
import geotrellis.spark.etl.config.backend.Backend
import geotrellis.spark.io.accumulo.{AccumuloAttributeStore, AccumuloWriteStrategy, HdfsWriteStrategy, SocketWriteStrategy}

import com.typesafe.scalalogging.slf4j.LazyLogging

trait AccumuloOutput[K, V, M] extends OutputPlugin[K, V, M] with LazyLogging {
  val name = "accumulo"
  val requiredKeys = Array("instance", "zookeepers", "user", "password", "table")

  def strategy(props: Map[String, String]): AccumuloWriteStrategy = {
    val strategy = props.get("strategy")
      .map {
        _ match {
          case "hdfs" => props.get("ingestPath") match {
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
  
  def attributes(props: Map[String, String], credentials: Option[Backend]) = AccumuloAttributeStore(getInstance(credentials).connector, props("table"))
}
