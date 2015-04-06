package geotrellis.spark.io.hadoop.spatial

import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.io.hadoop._
import geotrellis.spark.io.hadoop.formats._

import org.apache.spark.{SparkContext, Logging}
import org.apache.spark.rdd.RDD
import org.apache.hadoop.mapreduce.lib.input.SequenceFileInputFormat

// TODO: Refactor the writer and reader logic to abstract over the key type.
object SpatialRasterRDDReaderProvider extends RasterRDDReaderProvider[SpatialKey] with Logging {
  def reader(catalogConfig: HadoopRasterCatalogConfig, layerMetaData: HadoopLayerMetaData)(implicit sc: SparkContext): RasterRDDReader[SpatialKey] =
    new RasterRDDReader[SpatialKey] {
      def read(layerId: LayerId): RasterRDD[SpatialKey] = {
        val path = layerMetaData.path

        val dataPath = path.suffix(catalogConfig.SEQFILE_GLOB)

        logDebug(s"Loading $layerId from $dataPath")

        val conf = sc.hadoopConfiguration
        val inputConf = conf.withInputPath(dataPath)

        val writableRdd: RDD[(SpatialKeyWritable, TileWritable)] =
// TODO: Do we support filtering in HDFS?
//          if(filters.isEmpty) {
            sc.newAPIHadoopRDD[SpatialKeyWritable, TileWritable, SequenceFileInputFormat[SpatialKeyWritable, TileWritable]](
              inputConf,
              classOf[SequenceFileInputFormat[SpatialKeyWritable, TileWritable]],
              classOf[SpatialKeyWritable],
              classOf[TileWritable]
            )
          // } else {
          //   val partitioner = KeyPartitioner[K](readSplits(path, conf))

          //   val _filters = sc.broadcast(filters)
          //   val includeKey: keyWritable.Writable => Boolean =
          //   { writable =>
          //     _filters.value.includeKey(keyWritable.toValue(writable))
          //   }

          //   val includePartition: Partition => Boolean =
          //   { partition =>
          //     val minKey = partitioner.minKey(partition.index)
          //     val maxKey = partitioner.maxKey(partition.index)

          //     _filters.value.includePartition(minKey, maxKey)
          //   }

          //   new PreFilteredHadoopRDD[keyWritable.Writable, TileWritable](
          //     sc,
          //     classOf[SequenceFileInputFormat[keyWritable.Writable, TileWritable]],
          //     classTag[keyWritable.Writable].runtimeClass.asInstanceOf[Class[keyWritable.Writable]],
          //     classOf[TileWritable],
          //     inputConf
          //   )(includePartition)(includeKey)
          // }

        val rasterMetaData = layerMetaData.rasterMetaData

        asRasterRDD(rasterMetaData) {
          writableRdd
            .map  { case (keyWritable, tileWritable) =>
              (keyWritable.get, tileWritable.toTile(rasterMetaData))
          }
        }

      }
    }
}
