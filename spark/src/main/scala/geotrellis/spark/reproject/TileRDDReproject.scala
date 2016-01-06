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
    val layout = metadata.layout
    val mapTransform: MapKeyTransform = layout.mapTransform
    val tileLayout: TileLayout = layout.tileLayout

    val updatedOptions =
      options.parentGridExtent match {
        case Some(_) =>
          // Assume caller knows what she/he is doing
          options
        case None =>
          val parentGridExtent = ReprojectRasterExtent(layout.toGridExtent, crs, destCrs, options)
          options.copy(parentGridExtent = Some(parentGridExtent))
      }

    val reprojectedTiles =
      bufferedTiles
        .mapPartitions { partition =>
          val transform = Transform(crs, destCrs)
          val inverseTransform = Transform(destCrs, crs)

          partition.map { case (key, BufferedTile(tile, gridBounds)) =>
            val innerExtent = mapTransform(key)
            val innerRasterExtent = RasterExtent(innerExtent, gridBounds.width, gridBounds.height)
            val outerGridBounds =
              GridBounds(
                -gridBounds.colMin,
                -gridBounds.rowMin,
                tile.cols - gridBounds.colMin - 1,
                tile.rows - gridBounds.rowMin - 1
              )
            val outerExtent = innerRasterExtent.extentFor(outerGridBounds, clamp = false)

            val Product2(newTile, newExtent) =
              tile.reproject(outerExtent, gridBounds, transform, inverseTransform, updatedOptions)

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
          // Pad by one extra pixel.
          val e = re2.extent.reproject(destCrs, crs)
          val gb = re1.gridBoundsFor(e, clamp = false)
          BufferSizes(
            left = 1 + (if(gb.colMin < 0) -gb.colMin else 0),
            right = 1 + (if(gb.colMax >= re1.cols) gb.colMax - (re1.cols - 1) else 0),
            top = 1 + (if(gb.rowMin < 0) -gb.rowMin else 0),
            bottom = 1 + (if(gb.rowMax >= re1.rows) gb.rowMax - (re1.rows - 1) else 0)
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
    if(bufferSize == 0) {
      val fakeBuffers: RDD[(K, BufferedTile[V])] = rdd.withContext(_.mapValues { tile: V => BufferedTile(tile, GridBounds(0, 0, tile.cols - 1, tile.rows - 1)) })
      apply(fakeBuffers, rdd.metadata, destCrs, layoutScheme, options)
    } else
      apply(rdd.bufferTiles(bufferSize), rdd.metadata, destCrs, layoutScheme, options)
}
