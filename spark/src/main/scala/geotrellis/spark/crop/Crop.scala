package geotrellis.spark.crop

import geotrellis.raster._
import geotrellis.raster.crop.Crop.Options
import geotrellis.spark._
import geotrellis.util.MethodExtensions
import geotrellis.vector.Extent

import org.apache.spark.rdd._

object Crop {
  def apply[K: SpatialComponent](rdd: TileLayerRDD[K], extent: Extent, options: Options): TileLayerRDD[K] =
    rdd.metadata.extent.intersection(extent) match {
      case Some(intersectionExtent) =>
        rdd.metadata.getComponent[Bounds[K]] match {
          case kb @ KeyBounds(minKey, maxKey) =>
            val mapTransform = rdd.metadata.mapTransform

            val croppedRdd =
              rdd
                .mapPartitions({ partition =>
                  partition.flatMap({ case (key, tile) =>
                    val srcExtent = mapTransform(key)
                    if (extent.contains(srcExtent)) {
                      Some((key, tile))
                    } else if (extent.interiorIntersects(srcExtent)) {
                      val newTile = tile.crop(srcExtent, extent, options)
                      Some((key, newTile))
                    } else {
                      None
                    }
                  })
                }, preservesPartitioning = true)

            val GridBounds(minCol, minRow, maxCol, maxRow) =
              mapTransform(intersectionExtent)

            val newKeyBounds =
              KeyBounds(
                minKey.setComponent(SpatialKey(minCol, minRow)),
                maxKey.setComponent(SpatialKey(maxCol, maxRow))
              )

            val md = rdd.metadata.setComponent[Bounds[K]](newKeyBounds)

            ContextRDD(croppedRdd, md)
          case EmptyBounds =>
            rdd
        }
      case None =>
        ContextRDD(rdd.sparkContext.parallelize(Seq()), rdd.metadata.setComponent[Bounds[K]](EmptyBounds))
    }
}
