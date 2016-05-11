package geotrellis.spark.etl.accumulo

import com.typesafe.scalalogging.Logging
import com.typesafe.scalalogging.slf4j.LazyLogging
import geotrellis.spark.etl.OutputPlugin
import geotrellis.spark.io.accumulo.{ SocketWriteStrategy, HdfsWriteStrategy, AccumuloWriteStrategy, AccumuloAttributeStore }


trait AccumuloOutput[K, V, M] extends OutputPlugin[K, V, M] with LazyLogging {
  val name = "accumulo"
  val requiredKeys = Array("instance", "zookeeper", "user", "password", "table")

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
  
  def attributes(props: Map[String, String]) = AccumuloAttributeStore(getInstance(props).connector, props("table"))
}
