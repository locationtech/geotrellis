package geotrellis.spark.io.hadoop.geotiff

import geotrellis.raster.io.geotiff._
import geotrellis.raster.{CellGrid, Raster, RasterExtent}
import geotrellis.raster.resample.{RasterResampleMethods, ResampleMethod}
import geotrellis.spark.tiling.ZoomedLayoutScheme
import geotrellis.spark.{LayerId, SpatialKey}
import geotrellis.vector.{Extent, ProjectedExtent}
import geotrellis.raster.crop.Crop
import geotrellis.raster.reproject.Reproject.{Options => ReprojectOptions}
import geotrellis.raster.io.geotiff.reader.GeoTiffReader
import geotrellis.raster.reproject.RasterReprojectMethods
import geotrellis.raster.merge.RasterMergeMethods
import geotrellis.util.ByteReader

import scalaz.concurrent.{Strategy, Task}
import scalaz.stream.{Process, nondeterminism}

import java.net.URI
import java.util.concurrent.{ExecutorService, Executors}

import scala.reflect.ClassTag

trait GeoTiffLayerReader[M[T] <: Traversable[T]] {
  implicit def getByteReader(uri: URI): ByteReader

  val attributeStore: AttributeStore[M, GeoTiffMetadata]
  val layoutScheme: ZoomedLayoutScheme
  val resampleMethod: ResampleMethod
  val strategy: OverviewStrategy
  val defaultThreads: Int
  lazy val pool: ExecutorService = Executors.newFixedThreadPool(defaultThreads)

  def shutdown: Unit = pool.shutdown()

  def read[
    V <: CellGrid: GeoTiffReader: ClassTag
  ](layerId: LayerId)(x: Int, y: Int)(implicit rep: Raster[V] => RasterReprojectMethods[Raster[V]],
                                               res: Raster[V] => RasterResampleMethods[Raster[V]],
                                                 m: Raster[V] => RasterMergeMethods[V]): Raster[V] = {
    val layout =
      layoutScheme
        .levelForZoom(layerId.zoom)
        .layout

    val mapTransform = layout.mapTransform
    val keyExtent: Extent = mapTransform(SpatialKey(x, y))

    val index: Process[Task, GeoTiffMetadata] =
      Process
        .unfold(attributeStore.query(layerId.name, ProjectedExtent(keyExtent, layoutScheme.crs)).toIterator) { iter =>
          if (iter.hasNext) {
            val index: GeoTiffMetadata = iter.next()
            Some(index, iter)
          }
          else None
        }

    val readRecord: (GeoTiffMetadata => Process[Task, Option[Raster[V]]]) = { md =>
      Process eval Task {
        val tiff = GeoTiffReader[V].read(md.uri, decompress = false, streaming = true)
        val reprojectedKeyExtent = keyExtent.reproject(layoutScheme.crs, tiff.crs)

        // crop is unsafe, let's double check that we have a correct extent
        tiff
          .extent
          .intersection(reprojectedKeyExtent)
          .map { ext =>
            tiff
              .getClosestOverview(layout.cellSize, strategy)
              .crop(ext, Crop.Options(clamp = false))
              .raster
              .reproject(tiff.crs, layoutScheme.crs, ReprojectOptions(targetCellSize = Some(layout.cellSize)))
              .resample(RasterExtent(keyExtent, layoutScheme.tileSize, layoutScheme.tileSize))
          }
      }(pool)
    }

    nondeterminism
      .njoin(maxOpen = defaultThreads, maxQueued = defaultThreads) { index map readRecord }(Strategy.Executor(pool))
      .runLog.map(_.flatten.reduce(_ merge _)).unsafePerformSync
  }

  def readAll[
    V <: CellGrid: GeoTiffReader: ClassTag
  ](layerId: LayerId)(implicit rep: Raster[V] => RasterReprojectMethods[Raster[V]],
                               res: Raster[V] => RasterResampleMethods[Raster[V]]): Traversable[Raster[V]] = {
    val layout =
      layoutScheme
        .levelForZoom(layerId.zoom)
        .layout

    val index: Process[Task, GeoTiffMetadata] =
      Process
        .unfold(attributeStore.query(layerId.name).toIterator) { iter =>
          if (iter.hasNext) {
            val index: GeoTiffMetadata = iter.next()
            Some(index, iter)
          }
          else None
        }

    val readRecord: (GeoTiffMetadata => Process[Task, Raster[V]]) = { md =>
      Process eval Task {
        val tiff = GeoTiffReader[V].read(md.uri, decompress = false, streaming = true)
        tiff
          .crop(tiff.extent, layout.cellSize)
          .reproject(tiff.crs, layoutScheme.crs)
          .resample(layoutScheme.tileSize, layoutScheme.tileSize)
      }(pool)
    }

    nondeterminism
      .njoin(maxOpen = defaultThreads, maxQueued = defaultThreads) { index map readRecord }(Strategy.Executor(pool))
      .runLog.unsafePerformSync
  }
}
