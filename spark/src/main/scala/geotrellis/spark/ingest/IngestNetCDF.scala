package geotrellis.spark.ingest

import geotrellis.proj4.{LatLng, WebMercator, CRS}
import geotrellis.raster.{CellSize, Tile}
import geotrellis.spark._
import geotrellis.spark.io.hadoop.formats.NetCdfBand
import geotrellis.spark.io.hadoop._
import geotrellis.spark.tiling.{RowIndexScheme, LayoutLevel, TilingScheme}
import org.apache.spark._
import org.apache.spark.rdd.RDD
import geotrellis.raster.reproject._


/**
 * Ingests raw multi-band NetCDF tiles into a re-projected and tiled RasterRDD
 */
object IngestNetCDF extends Logging {
  type Source = RDD[(NetCdfBand, Tile)]
  type Sink = RasterRDD[TimeTileId] => Unit


  def apply (sc: SparkContext)(source: Source, sink:  Sink, destCRS: CRS, tilingScheme: TilingScheme = TilingScheme.TMS): Unit = {
  import geotrellis.vector.reproject._

    val reprojected = source.map {
      case (band, tile) =>
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

      val layerLevel: LayoutLevel = tilingScheme.layoutFor(band.crs, CellSize(band.extent, dims))
      LayerMetaData(cellType, band.extent, band.crs, layerLevel, RowIndexScheme)
    }

    val raster =
      new RasterRDD(reprojected, metaData) // WARNING: I'm discarding band.varName here // TODO deal with this
        .mosaic(extentOf = _.extent, toKey = (band, tileId) => TimeTileId(tileId, band.time))

    sink(raster)
  }
}
