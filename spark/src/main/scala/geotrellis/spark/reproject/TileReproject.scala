package geotrellis.spark.reproject

import geotrellis.spark.buffer._
import geotrellis.proj4._
import geotrellis.raster._
import geotrellis.raster.reproject._
import geotrellis.raster.resample._
import geotrellis.spark._
import geotrellis.spark.op._
import geotrellis.spark.ingest._
import geotrellis.vector._
import geotrellis.vector.reproject._

import org.apache.spark.rdd._
import org.apache.spark.storage.StorageLevel

import scala.reflect.ClassTag

object TileReproject {
  type Apply[K] = geotrellis.raster.reproject.Reproject.Apply[RasterRDD[K]]

  def apply[K: SpatialComponent: ClassTag](
      rdd: RasterRDD[K],
      destCRS: CRS): Apply[K] = 
    new Apply[K] {
      def apply(method: ResampleMethod = NearestNeighbor, errorThreshold: Double = 0.125): RasterRDD[K] = {
        val crs = rdd.metaData.crs
        val mapTransform = rdd.metaData.layout.mapTransform
        val tileLayout = rdd.metaData.layout.tileLayout

        val transform = Transform(crs, destCRS)
        val inverseTransform = Transform(destCRS, crs)

        val rasterExtents: RDD[(K, (RasterExtent, RasterExtent))] = 
          rdd
            .mapPartitions({ partition =>
              partition.map { case (key, _) =>
                val extent = mapTransform(key)
                val rasterExtent = RasterExtent(extent, tileLayout.tileCols, tileLayout.tileRows)
                (key, (rasterExtent, ReprojectRasterExtent(rasterExtent, transform)))
              }
            }, preservesPartitioning = true)
            .persist(StorageLevel.MEMORY_ONLY)

        // Find Extent and CellSize for RasterMetaData
        val (newExtent, newCellSize) =
          rasterExtents
            .map { case (_, (_, rasterExtent)) =>
              (rasterExtent.extent, CellSize(rasterExtent.cellwidth, rasterExtent.cellheight))
            }
            .reduce { (t1, t2) =>
              val (e1, cs1) = t1
              val (e2, cs2) = t2
              (
                e1.combine(e2),
                if (cs1.resolution < cs2.resolution) cs1 else cs2
              )
            }

        val borderSizesPerKey =
          rasterExtents
            .mapValues { case (re1, re2) =>
              // Reproject the extent back into the original CRS,
              // to determine how many border pixels we need.
              val e = re2.extent.reproject(inverseTransform)
              val gb = re1.gridBoundsFor(e, clamp = false)
              BorderSizes(
                left = if(gb.colMin < 0) -gb.colMin else 0,
                right = if(gb.colMax >= re1.cols) gb.colMax - (re1.cols - 1) else 0,
                top = if(gb.rowMin < 0) -gb.rowMin else 0,
                bottom = if(gb.rowMax >= re1.rows) gb.rowMax - (re1.rows - 1) else 0
              )
            }
            .persist(StorageLevel.MEMORY_ONLY)

        val reprojectedTiles =
          rdd
            .bufferTiles(borderSizesPerKey)
            .map { case (key, BufferedTile(tile, gridBounds)) =>
              val extent = mapTransform(key)
              val Raster(newTile, newExtent) = Raster(tile, extent).reproject(gridBounds, transform, inverseTransform)(method = method, errorThreshold = errorThreshold)
              ((key, newExtent), newTile)
            }

        val metadata =
          RasterMetaData.fromRdd(reprojectedTiles, destCRS, rdd.metaData.layout) { key => key._2 }

        val tiler: Tiler[(K, Extent), K, Tile] = {
          val getExtent = (inKey: (K, Extent)) => inKey._2
          val createKey = (inKey: (K, Extent), spatialComponent: SpatialKey) => inKey._1.updateSpatialComponent(spatialComponent)
          Tiler(getExtent, createKey)
        }

        RasterRDD(tiler(reprojectedTiles, metadata, method), metadata)
      }

      // def apply(method: ResampleMethod = NearestNeighbor, errorThreshold: Double = 0.125): RasterRDD[K] = {
      //   val crs = rdd.metaData.crs
      //   val mapTransform = rdd.metaData.layout.mapTransform
      //   val tileLayout = rdd.metaData.layout.tileLayout

      //   val reprojectedTiles =
      //     rdd.map { case (key, tile) =>
      //       val extent = mapTransform(key)
      //       val Raster(newTile, newExtent) = tile.reproject(extent, crs, destCRS)(method = method, errorThreshold = errorThreshold)
      //       ((key, newExtent), newTile)
      //     }

      //   val metadata =
      //     RasterMetaData.fromRdd(reprojectedTiles, destCRS, rdd.metaData.layout) { key => key._2 }

      //   val tiler: Tiler[(K, Extent), K, Tile] = {
      //     val getExtent = (inKey: (K, Extent)) => inKey._2
      //     val createKey = (inKey: (K, Extent), spatialComponent: SpatialKey) => inKey._1.updateSpatialComponent(spatialComponent)
      //     Tiler(getExtent, createKey)
      //   }

      //   new RasterRDD(tiler(reprojectedTiles, metadata, method), metadata)
      // }
    }

}
