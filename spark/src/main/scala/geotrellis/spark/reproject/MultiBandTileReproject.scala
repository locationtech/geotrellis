package geotrellis.spark.reproject

import geotrellis.spark.buffer._
import geotrellis.proj4._
import geotrellis.raster._
import geotrellis.raster.reproject._
import geotrellis.raster.resample._
import geotrellis.spark._
import geotrellis.spark.op._
import geotrellis.spark.ingest._
import geotrellis.spark.tiling._
import geotrellis.vector._
import geotrellis.vector.reproject._

import org.apache.spark.rdd._
import org.apache.spark.storage.StorageLevel

import scala.reflect.ClassTag

object MultiBandTileReproject {
  type Apply[K] = geotrellis.raster.reproject.Reproject.Apply[(Int, MultiBandRasterRDD[K])]

  private def apply[K: SpatialComponent: ClassTag](
    rdd: MultiBandRasterRDD[K],
    destCRS: CRS,
    layoutScheme: LayoutScheme,
    method: ResampleMethod,
    errorThreshold: Double
  ): (Int, MultiBandRasterRDD[K]) = {
    val srcCrs = rdd.metaData.crs
    val destCrs = destCRS
    val mapTransform = rdd.metaData.layout.mapTransform
    val tileLayout = rdd.metaData.layout.tileLayout

    val rasterExtents: RDD[(K, (RasterExtent, RasterExtent))] =
      rdd
        .mapPartitions({ partition =>
          val transform = Transform(srcCrs, destCrs)

          partition.map { case (key, _) =>
            val extent = mapTransform(key)
            val rasterExtent = RasterExtent(extent, tileLayout.tileCols, tileLayout.tileRows)
            (key, (rasterExtent, ReprojectRasterExtent(rasterExtent, transform)))
          }
        }, preservesPartitioning = true)

    val borderSizesPerKey =
      rasterExtents
        .mapValues { case (re1, re2) =>
          // Reproject the extent back into the original CRS,
          // to determine how many border pixels we need.
          val e = re2.extent.reproject(destCrs, srcCrs)
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
          val transform = Transform(srcCrs, destCrs)
          val inverseTransform = Transform(destCrs, srcCrs)

          // We need to determine the extent of the tile with it's borders in order to
          // correctly do the windowed reprojection
          val innerExtent = mapTransform(key)
          val innerRasterExtent = RasterExtent(innerExtent, gridBounds.width, gridBounds.height)
          val outerGridBounds = GridBounds(-gridBounds.colMin, -gridBounds.rowMin, tile.cols - gridBounds.colMin - 1, tile.rows - gridBounds.rowMin - 1)
          val outerExtent = innerRasterExtent.extentFor(outerGridBounds, clamp = false)

          val MultiBandRaster(newTile, newExtent) =
            MultiBandRaster(tile, outerExtent).reproject(gridBounds, transform, inverseTransform)(method = method, errorThreshold = errorThreshold)
          ((key, newExtent), newTile)
        }

    val (zoom, metadata) =
      RasterMetaData.fromRdd(reprojectedTiles, destCrs, layoutScheme) { key => key._2 }

    val tiler: Tiler[(K, Extent), K, MultiBandTile] = {
      val getExtent = (inKey: (K, Extent)) => inKey._2
      val createKey = (inKey: (K, Extent), spatialComponent: SpatialKey) => inKey._1.updateSpatialComponent(spatialComponent)
      Tiler(getExtent, createKey)
    }

    (zoom, MultiBandRasterRDD(tiler(reprojectedTiles, metadata, method), metadata))
  }

  def apply[K: SpatialComponent: ClassTag](
    rdd: MultiBandRasterRDD[K],
    destCRS: CRS,
    layoutScheme: LayoutScheme
  ): Apply[K] =
    new Apply[K] {
      def apply(method: ResampleMethod = NearestNeighbor, errorThreshold: Double = 0.125): (Int, MultiBandRasterRDD[K]) =
        MultiBandTileReproject.apply(rdd, destCRS, layoutScheme, method, errorThreshold)
    }
}
