package geotrellis.spark.io.cassandra

import geotrellis.spark.io._

import com.datastax.spark.connector.cql._
import org.apache.spark.SparkContext

case class CassandraInstance(
  connector: CassandraConnector, 
  keyspace: String
) {
  
  val session = connector.openSession()
  val catalogTable = "metadata"  // TODO: Move into config file
  val metaDataCatalog = new CassandraMetaDataCatalog(session, keyspace, catalogTable)
  
  def catalog(config: DefaultParams[String] = CassandraCatalog.BaseParamsConfig)(implicit sc: SparkContext) =
    CassandraCatalog(sc, session, keyspace, metaDataCatalog, config)

  def catalog(implicit sc: SparkContext) =
    CassandraCatalog(sc, session, keyspace, metaDataCatalog, CassandraCatalog.BaseParamsConfig)
}
