package geotrellis.spark.ingest

import geotrellis.spark._
import geotrellis.spark.tiling._
import geotrellis.spark.io.hadoop._
import geotrellis.spark.io.hadoop.formats.NetCdfBand

import geotrellis.raster._
import geotrellis.raster.reproject._
import geotrellis.proj4.CRS

import org.apache.spark._
import org.apache.spark.rdd._

/**
 * Ingests raw multi-band NetCDF tiles into a re-projected and tiled RasterRDD
 */
object NetCDFIngest extends Logging {
  type Source = RDD[(NetCdfBand, Tile)]
  type Sink = RasterRDD[SpaceTimeKey] => Unit

  def apply (sc: SparkContext)(layerName: String, source: Source, sink:  Sink, destCRS: CRS, layoutScheme: LayoutScheme = ZoomedLayoutScheme()): Unit = {
    val reprojected = 
      source.map { case (band, tile) =>
        val (reprojectedTile, reprojectedExtent) = tile.reproject(band.extent, band.crs, destCRS)
        band.copy(crs = destCRS, extent = reprojectedExtent) -> reprojectedTile
      }

    // First step is to build metaData
    val metaData: LayerMetaData = {
      // Note: dimensions would have changed from the stored if we actually reprojected
      val (band, dims, cellType) =
        reprojected.map {
          case (band, tile) => (band, tile.dimensions, tile.cellType)
        }.first // HERE we make an assumption that all bands share the same extent, therefor the first will do

      val layerLevel: LayoutLevel = layoutScheme.levelFor(band.extent, CellSize(band.extent, dims))
      LayerMetaData(
        LayerId(layerName, layerLevel.zoom),
        RasterMetaData(cellType, band.extent, band.crs, layerLevel.tileLayout)
      )
    }

//    import SpaceTimeKey._

    val retiled = 
      reprojected
        .retile[SpaceTimeKey](metaData.rasterMetaData)(_.extent)((band, tileId) => SpaceTimeKey(tileId, band.time))

    val raster = new RasterRDD(retiled, metaData.rasterMetaData)

    sink(raster)
  }
}
