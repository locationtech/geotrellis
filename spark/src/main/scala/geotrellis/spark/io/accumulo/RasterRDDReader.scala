package geotrellis.spark.io.accumulo

import geotrellis.spark._
import geotrellis.spark.utils._
import geotrellis.spark.io._
import geotrellis.spark.io.index._
import geotrellis.raster._
import org.apache.spark.rdd.RDD
import org.apache.accumulo.core.client.mapreduce.InputFormatBase
import org.apache.spark.SparkContext
import org.apache.hadoop.mapreduce.Job
import org.apache.accumulo.core.data.{Key, Value, Range => ARange}

import scala.reflect.ClassTag

abstract class RasterRDDReader[K: ClassTag] {

  def getCube(
    job: Job,
    layerId: LayerId,
    keyBounds: KeyBounds[K],
    keyIndex: KeyIndex[K]
  )(implicit sc: SparkContext): RDD[(Key, Value)]

  def read(instance: AccumuloInstance,
    metadata: AccumuloLayerMetaData,
    keyBounds: KeyBounds[K],
    index: KeyIndex[K]
  )(layerId: LayerId, 
    queryKeyBounds: Seq[KeyBounds[K]]
  )(implicit sc: SparkContext): RasterRDD[K] = {
    val AccumuloLayerMetaData(_, rasterMetaData, tileTable) = metadata

    val tileRdd = 
      queryKeyBounds
      .map{ subKeyBound => 
        val job = Job.getInstance(sc.hadoopConfiguration)  
        instance.setAccumuloConfig(job)
        InputFormatBase.setInputTableName(job, tileTable)        
        getCube(job, layerId, subKeyBound, index)        
      }
      .reduce(_ union _)
      .map { case (akey, value) =>
        val (key, tileBytes) = KryoSerializer.deserialize[(K, Array[Byte])](value.get)
        val tile =
          ArrayTile.fromBytes(
            tileBytes,
            rasterMetaData.cellType,
            rasterMetaData.tileLayout.tileCols,
            rasterMetaData.tileLayout.tileRows)
        (key, tile: Tile)

      }

    new RasterRDD(tileRdd, rasterMetaData)
  }
}
