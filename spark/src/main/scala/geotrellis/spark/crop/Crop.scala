package geotrellis.spark.crop

import geotrellis.raster._
import geotrellis.raster.crop.Crop.Options
import geotrellis.spark._
import geotrellis.util.MethodExtensions
import geotrellis.vector.Extent

import org.apache.spark.rdd._

object Crop {
  def apply[K: SpatialComponent](rdd: TileLayerRDD[K], extent: Extent, options: Options): TileLayerRDD[K] = {
    val md = rdd.metadata
    val mt = md.mapTransform
    val croppedRdd =
      rdd
        .mapPartitions({ partition =>
          partition.flatMap({ case (key, tile) =>
            val srcExtent = mt(key)
            if (extent.contains(srcExtent))
              Some((key, tile))
            else if (extent.interiorIntersects(srcExtent)) {
              val newTile = tile.crop(srcExtent, extent, options)
              Some((key, newTile))
            }
            else None
          })
        }, preservesPartitioning = true)

    ContextRDD(croppedRdd, md)
  }
}
