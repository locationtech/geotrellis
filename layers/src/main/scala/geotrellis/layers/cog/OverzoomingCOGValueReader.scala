package geotrellis.layers.cog

import geotrellis.tiling.{SpatialComponent, SpatialKey, ZoomedLayoutScheme}
import geotrellis.raster.{CellGrid, RasterExtent, Tile}
import geotrellis.raster.io.geotiff.reader.GeoTiffReader
import geotrellis.raster.resample.{ResampleMethod, TileResampleMethods}
import geotrellis.layers.{LayerId, TileLayerMetadata}
import geotrellis.layers._
import geotrellis.util._
import spray.json._

import scala.reflect.ClassTag


trait OverzoomingCOGValueReader extends COGValueReader[LayerId] {
  implicit def getLayerId(id: LayerId): LayerId = id

  def overzoomingReader[
    K: JsonFormat: SpatialComponent: ClassTag,
    V <: CellGrid[Int]: GeoTiffReader: ? => TileResampleMethods[V]
  ](layerId: LayerId, resampleMethod: ResampleMethod): COGReader[K, V] = new COGReader[K, V] {
    val LayerId(layerName, requestedZoom) = layerId
    val maxAvailableZoom = attributeStore.layerIds.filter { case LayerId(name, _) => name == layerName }.map(_.zoom).max
    val metadata = attributeStore.readMetadata[TileLayerMetadata[K]](LayerId(layerName, maxAvailableZoom))

    val layoutScheme = ZoomedLayoutScheme(metadata.crs, metadata.tileRows)
    val requestedMaptrans = layoutScheme.levelForZoom(requestedZoom).layout.mapTransform
    val maxMaptrans = metadata.mapTransform

    lazy val baseReader = reader[K, V](layerId)
    lazy val maxReader = reader[K, V](LayerId(layerName, maxAvailableZoom))

    def deriveMaxKey(key: K): K = {
      val srcSK = key.getComponent[SpatialKey]
      val denom = math.pow(2, requestedZoom - maxAvailableZoom).toInt
      key.setComponent[SpatialKey](SpatialKey(srcSK._1 / denom, srcSK._2 / denom))
    }

    def readSubsetBands(key: K, bands: Seq[Int]): Array[Option[Tile]] =
      if (requestedZoom <= maxAvailableZoom) {
        baseReader.readSubsetBands(key, bands)
      } else {
        val maxKey = deriveMaxKey(key)
        val toResamples = maxReader.readSubsetBands(maxKey, bands)

        toResamples.map { toResample =>
          toResample match {
            case None => None
            case Some(tile) =>
              Some(
                tile.resample(
                  maxMaptrans.keyToExtent(maxKey.getComponent[SpatialKey]),
                  RasterExtent(requestedMaptrans.keyToExtent(key.getComponent[SpatialKey]), tile.cols, tile.rows),
                  resampleMethod
                )
              )
          }
        }
      }

    def read(key: K): V =
      if (requestedZoom <= maxAvailableZoom) {
        baseReader.read(key)
      } else {
        val maxKey = deriveMaxKey(key)
        val toResample = maxReader.read(maxKey)

        toResample.resample(
          maxMaptrans.keyToExtent(maxKey.getComponent[SpatialKey]),
          RasterExtent(requestedMaptrans.keyToExtent(key.getComponent[SpatialKey]), toResample.cols, toResample.rows),
          resampleMethod
        )
      }
  }
}
