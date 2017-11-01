package geotrellis.spark.io.hadoop.geotiff

import geotrellis.raster.io.geotiff._
import geotrellis.raster.{Raster, RasterExtent, Tile}
import geotrellis.raster.resample.ResampleMethod
import geotrellis.spark.tiling.ZoomedLayoutScheme
import geotrellis.spark.{LayerId, SpatialKey}
import geotrellis.vector.{Extent, ProjectedExtent}
import geotrellis.raster.crop.Crop
import geotrellis.raster.reproject.Reproject.{Options => ReprojectOptions}

import java.net.URI

// global context only for test purposes, should be refactored
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.blocking
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

/** Approach with TiffTags stored in a DB */
trait GeoTiffLayerReader[M[T] <: Traversable[T]] {
  val attributeStore: AttributeStore[M, GeoTiffMetadata]
  val layoutScheme: ZoomedLayoutScheme
  val resampleMethod: ResampleMethod
  val strategy: OverviewStrategy

  protected def readSingleband(uri: URI): SinglebandGeoTiff

  def read(layerId: LayerId)(x: Int, y: Int): Raster[Tile] = {
    val layout =
      layoutScheme
        .levelForZoom(layerId.zoom)
        .layout

    val mapTransform = layout.mapTransform
    val keyExtent: Extent = mapTransform(SpatialKey(x, y))

    Await
      .result(Future.sequence(attributeStore
        .query(layerId.name, ProjectedExtent(keyExtent, layoutScheme.crs))
        .map { md =>
          Future {
            blocking {
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
            }
          }
        }
      )
      .map(_.flatten.reduce(_ merge _)), Duration.Inf)
  }

  def readAll(layerId: LayerId): Traversable[Raster[Tile]] = {
    val layout =
      layoutScheme
        .levelForZoom(layerId.zoom)
        .layout

    Await.result(Future.sequence(attributeStore
      .query(layerId.name)
      .map { md => Future { blocking {
        val tiff = readSingleband(md.uri)
        tiff
          .crop(tiff.extent, layout.cellSize)
          .reproject(tiff.crs, layoutScheme.crs)
          .resample(layoutScheme.tileSize, layoutScheme.tileSize)
      } } }), Duration.Inf)
  }
}
