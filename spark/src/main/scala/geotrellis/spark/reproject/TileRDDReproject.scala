package geotrellis.spark.reproject

import geotrellis.spark.buffer._
import geotrellis.proj4._
import geotrellis.raster._
import geotrellis.raster.reproject._
import geotrellis.raster.resample._
import geotrellis.raster.crop._
import geotrellis.raster.mosaic._
import geotrellis.raster.stitch._
import geotrellis.spark._
import geotrellis.spark.op._
import geotrellis.spark.ingest._
import geotrellis.spark.tiling._
import geotrellis.vector._
import geotrellis.vector.reproject._

import org.apache.spark.rdd._
import org.apache.spark.storage.StorageLevel

import scala.reflect.ClassTag

object TileRDDReproject {
  import geotrellis.raster.reproject.Reproject.Options

  def apply[
    K: SpatialComponent: ClassTag,
    V <: CellGrid: ClassTag: Stitcher: (? => TileReprojectMethods[V]): (? => CropMethods[V]): (? => MergeMethods[V]): (? => CellGridPrototype[V])
  ](
    bufferedTiles: RDD[(K, BufferedTile[V])],
    metadata: RasterMetaData,
    destCrs: CRS,
    layoutScheme: LayoutScheme,
    options: Options
  ): (Int, RDD[(K, V)] with Metadata[RasterMetaData]) = {
    val crs: CRS = metadata.crs
    val mapTransform: MapKeyTransform = metadata.layout.mapTransform
    val tileLayout: TileLayout = metadata.layout.tileLayout

    val reprojectedTiles =
      bufferedTiles
        .mapPartitions { partition =>
          val transform = Transform(crs, destCrs)
          val inverseTransform = Transform(destCrs, crs)

          partition.map { case (key, BufferedTile(tile, gridBounds)) =>
            val extent = mapTransform(key)

            val innerExtent = mapTransform(key)
            val innerRasterExtent = RasterExtent(innerExtent, gridBounds.width, gridBounds.height)
            val outerGridBounds = GridBounds(-gridBounds.colMin, -gridBounds.rowMin, tile.cols - gridBounds.colMin - 1, tile.rows - gridBounds.rowMin - 1)
            val outerExtent = innerRasterExtent.extentFor(outerGridBounds, clamp = false)

            val Product2(newTile, newExtent) =
              tile.reproject(outerExtent, gridBounds, transform, inverseTransform, options)
            ((key, newExtent), newTile)
          }
        }

    val (zoom, newMetadata) =
      RasterMetaData.fromRdd(reprojectedTiles, destCrs, layoutScheme) { key => key._2 }

    val tiler: Tiler[(K, Extent), K, V] = {
      val getExtent = (inKey: (K, Extent)) => inKey._2
      val createKey = (inKey: (K, Extent), spatialComponent: SpatialKey) => inKey._1.updateSpatialComponent(spatialComponent)
      Tiler(getExtent, createKey)
    }

    (zoom, ContextRDD(tiler(reprojectedTiles, newMetadata, options.method), newMetadata))
  }

  def apply[
    K: SpatialComponent: ClassTag,
    V <: CellGrid: ClassTag: Stitcher: (? => TileReprojectMethods[V]): (? => CropMethods[V]): (? => MergeMethods[V]): (? => CellGridPrototype[V])
  ](
    rdd: RDD[(K, V)] with Metadata[RasterMetaData],
    destCrs: CRS,
    layoutScheme: LayoutScheme,
    options: Options
  ): (Int, RDD[(K, V)] with Metadata[RasterMetaData]) = {
    val crs = rdd.metaData.crs
    val mapTransform = rdd.metaData.layout.mapTransform
    val tileLayout = rdd.metaData.layout.tileLayout

    val rasterExtents: RDD[(K, (RasterExtent, RasterExtent))] =
      rdd
        .mapPartitions({ partition =>
          val transform = Transform(crs, destCrs)

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
          val e = re2.extent.reproject(destCrs, crs)
          val gb = re1.gridBoundsFor(e, clamp = false)
          BufferSizes(
            left = if(gb.colMin < 0) -gb.colMin else 0,
            right = if(gb.colMax >= re1.cols) gb.colMax - (re1.cols - 1) else 0,
            top = if(gb.rowMin < 0) -gb.rowMin else 0,
            bottom = if(gb.rowMax >= re1.rows) gb.rowMax - (re1.rows - 1) else 0
          )
        }

    val bufferedTiles =
      rdd.bufferTiles(borderSizesPerKey)

    apply(bufferedTiles, rdd.metadata, destCrs, layoutScheme, options)
  }

  def apply[
    K: SpatialComponent: ClassTag,
    V <: CellGrid: ClassTag: Stitcher: (? => TileReprojectMethods[V]): (? => CropMethods[V]): (? => MergeMethods[V]): (? => CellGridPrototype[V])
  ](
    rdd: RDD[(K, V)] with Metadata[RasterMetaData],
    destCrs: CRS,
    layoutScheme: LayoutScheme,
    bufferSize: Int,
    options: Options
  ): (Int, RDD[(K, V)] with Metadata[RasterMetaData]) =
    apply(rdd.bufferTiles(bufferSize), rdd.metadata, destCrs, layoutScheme, options)
}
