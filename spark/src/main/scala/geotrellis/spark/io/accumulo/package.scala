package geotrellis.spark.io

import geotrellis.spark._
import geotrellis.spark.tiling._
import geotrellis.spark.rdd._
import geotrellis.raster._

import org.apache.spark._
import org.apache.spark.rdd._
import org.apache.hadoop.mapreduce.Job

import org.apache.accumulo.core.client.security.tokens.AuthenticationToken
import org.apache.accumulo.core.client.{BatchWriterConfig, Connector}
import org.apache.accumulo.core.client.mapreduce.{InputFormatBase, AccumuloInputFormat}
import org.apache.accumulo.core.data.{Key, Value, Range => ARange}
import org.apache.accumulo.core.client.mapreduce.lib.util.{ConfiguratorBase => CB}

import scala.collection.JavaConversions._

package object accumulo {

  implicit class AccumuloLoadFunctions(sc: SparkContext)
  {
    def setZooKeeperInstance(instance: String, hosts: String) =
      CB.setZooKeeperInstance(classOf[AccumuloInputFormat], sc.hadoopConfiguration, instance, hosts)

    def setAccumuloCredential(user: String, token: AuthenticationToken) =
      CB.setConnectorInfo(classOf[AccumuloInputFormat], sc.hadoopConfiguration, user, token)

    /**
     * @param table   name of the accumulo table
     * @param layer   name of the layer, "nlcd-2011:12"
     *                this encodes both the column family and the zoom level
     */
    def accumuloRDD[K, L](table: String, layer: L)
      (implicit format: AccumuloFormat[K, L]): RDD[(K, Tile)] =
      accumuloRDD(table, layer, None)(format)

    def accumuloRasterRDD[K, L](table: String, layer: L)
      (implicit format: AccumuloFormat[K, L]): RDD[(K, Tile)] =
      accumuloRDD(table, layer)(format)

    /**
     * @param table   name of the accumulo table
     * @param layer   name of the layer, "nlcd-2011:12"
     *                this encodes both the column family and the zoom level
     * @param extent  tile extent, will usually be used to refine row selection
     */
    def accumuloRasterRDD[K, L](table: String, layer: L, gridBounds: GridBounds)
      (implicit format: AccumuloFormat[K, L]): RDD[(K, Tile)] =
      accumuloRasterRDD(table, layer, gridBounds, GridCoordScheme)(format)

    /**
     * @param table     name of the accumulo table
     * @param layer     name of the layer, "nlcd-2011:12"
     *                  this encodes both the column family and the zoom level
     * @param rowSpans  option sequence of row spans, will usually be used to refine row selection
     */
    def accumuloRasterRDD[K, L](table: String, layer: L, tileBounds: TileBounds, coordScheme: TileCoordScheme)
      (implicit format: AccumuloFormat[K, L]): RDD[(K, Tile)] =
    {
      // TODO: Read metadata, so we can figure out how the grid coordinates translate to TileIds
      val metaData: LayerMetaData = ???

      val spans = 
        metaData.transform.withCoordScheme(coordScheme).tileToIndex(tileBounds).spans

      accumuloRDD(table, layer, Some(spans))(format)
    }

    def accumuloRDD[K, L](table: String, layer: L, spans: Option[Seq[(Long, Long)]])
      (implicit format: AccumuloFormat[K, L]): RDD[(K, Tile)] = {

      val job = Job.getInstance(sc.hadoopConfiguration)
      InputFormatBase.setInputTableName(job, table)
      InputFormatBase.setRanges(job, format.ranges(layer, spans))

      // TODO: Set some filters here to represent a query

      sc.newAPIHadoopRDD(
        job.getConfiguration, classOf[AccumuloInputFormat], classOf[Key], classOf[Value]
      ).map { case (key, value) => format.read(key,value)}
    }
  }

  implicit class AccumuloSaveFunctions[K](rdd: RasterRDD) {
    def saveAccumulo[L](table: String, layer: L, accumuloConnector: Connector)
                    (implicit format: AccumuloFormat[TileId, L]): Unit =
    {
      val sc = rdd.sparkContext
      val connectorBC = sc.broadcast(accumuloConnector)

      sc.runJob(rdd, { partition: Iterator[(K, Tile)] =>
        val cfg = new BatchWriterConfig()
        val writer = connectorBC.value.createBatchWriter(table, cfg)

        partition.foreach { row =>
          writer.addMutation(format.write(layer, row))
        }
        writer.close()
      })
    }
  }
}
