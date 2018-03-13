package geotrellis.spark.io.hadoop.geotiff

import geotrellis.raster.io.geotiff._
import geotrellis.raster.{Raster, RasterExtent, Tile}
import geotrellis.raster.resample.ResampleMethod
import geotrellis.spark.tiling.ZoomedLayoutScheme
import geotrellis.spark.{LayerId, SpatialKey}
import geotrellis.vector.{Extent, ProjectedExtent}
import geotrellis.raster.crop.Crop
import geotrellis.raster.reproject.Reproject.{Options => ReprojectOptions}

import scalaz.concurrent.{Strategy, Task}
import scalaz.stream.{Process, nondeterminism}

import java.net.URI
import java.util.concurrent.{ExecutorService, Executors}

/** Approach with TiffTags stored in a DB */
trait GeoTiffLayerReader[M[T] <: Traversable[T]] {
  val attributeStore: AttributeStore[M, GeoTiffMetadata]
  val layoutScheme: ZoomedLayoutScheme
  val resampleMethod: ResampleMethod
  val strategy: OverviewStrategy
  val defaultThreads: Int
  lazy val pool: ExecutorService = Executors.newFixedThreadPool(defaultThreads)

  def shutdown: Unit = pool.shutdown()

  protected def readSingleband(uri: URI): SinglebandGeoTiff

  def read(layerId: LayerId)(x: Int, y: Int): Raster[Tile] = {
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

    val readRecord: (GeoTiffMetadata => Process[Task, Option[Raster[Tile]]]) = { md =>
      Process eval Task {
        val tiff = readSingleband(md.uri)
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

  def readAll(layerId: LayerId): Traversable[Raster[Tile]] = {
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

    val readRecord: (GeoTiffMetadata => Process[Task, Raster[Tile]]) = { md =>
      Process eval Task {
        val tiff = readSingleband(md.uri)
        tiff
          .crop(tiff.extent, layout.cellSize)
          .reproject(tiff.crs, layoutScheme.crs)
          .resample(layoutScheme.tileSize, layoutScheme.tileSize)
      } (pool)
    }

    nondeterminism
      .njoin(maxOpen = defaultThreads, maxQueued = defaultThreads) { index map readRecord }(Strategy.Executor(pool))
      .runLog.unsafePerformSync
  }
}
