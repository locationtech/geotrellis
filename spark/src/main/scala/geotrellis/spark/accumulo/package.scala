package geotrellis.spark

import org.apache.accumulo.core.client.{BatchWriterConfig, Connector}
import org.apache.accumulo.core.client.mapreduce.{InputFormatBase, AccumuloInputFormat}
import org.apache.accumulo.core.data.{Key, Value, Range => ARange}
import org.apache.hadoop.mapreduce.Job
import org.apache.spark._
import org.apache.spark.rdd._
import scala.collection.JavaConversions._
import geotrellis.raster._

import scala.reflect.ClassTag


package object accumulo {

  implicit class AccumuloLoadFunctions(sc: SparkContext)
  {
    def accumuloRDD[K: TileAccumuloFormat](table: String): RDD[(K, Tile)] = {
      val job = new Job(sc.hadoopConfiguration)
      InputFormatBase.setInputTableName(job, table)
      InputFormatBase.setRanges(job, new ARange() :: Nil)

      //If I had query parameters I would have to use InputFormatBase
      // to declare some iterators and set some ARanges
      // So where should they come from ?

      val format = implicitly[TileAccumuloFormat[K]]
      sc.newAPIHadoopRDD(
        job.getConfiguration, classOf[AccumuloInputFormat], classOf[Key], classOf[Value]
      ).map { case (key, value) => format.read(key,value)}
    }
  }

  implicit class AccumuloSaveFunctions[K: ClassTag](rdd: RDD[(K, Tile)])
  {
    def saveAccumulo(table: String, accumuloConnector: Connector)
                    (implicit format: TileAccumuloFormat[K]): Unit =
    {
      val sc = rdd.sparkContext
      val connectorBC = sc.broadcast(accumuloConnector)
      sc.runJob(rdd, { partition: Iterator[(K, Tile)] =>
        val cfg = new BatchWriterConfig()
        val writer = connectorBC.value.createBatchWriter(table, cfg)

        partition.foreach { row =>
          writer.addMutation(format.write(row))
        }
        writer.close
      })
    }
  }

}
