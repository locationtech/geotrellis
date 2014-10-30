package geotrellis.spark.ingest

import geotrellis.spark._
import geotrellis.vector.Extent
import geotrellis.proj4._
import geotrellis.spark.tiling._
import geotrellis.spark.io.accumulo._
import geotrellis.spark.io.hadoop._
import geotrellis.spark.io.hadoop.formats.NetCdfBand
import org.apache.hadoop.fs._
import org.apache.accumulo.core.client.security.tokens.PasswordToken

import geotrellis.raster._
import geotrellis.raster.reproject._
import geotrellis.proj4.CRS

import org.apache.spark._
import org.apache.spark.rdd._

import com.quantifind.sumac.ArgMain

/**
 * Ingests raw multi-band NetCDF tiles into a re-projected and tiled RasterRDD
 */

class NetCdfIngest(catalog: AccumuloCatalog, layoutScheme: LayoutScheme)(implicit tiler: Tiler[NetCdfBand, SpaceTimeKey])
    extends AccumuloIngest[NetCdfBand, SpaceTimeKey](catalog, layoutScheme) {
  override def isUniform = true
}

object NetCDFIngestCommand extends ArgMain[AccumuloIngestArgs] with Logging {
  def main(args: AccumuloIngestArgs): Unit = {
    System.setProperty("com.sun.media.jai.disableMediaLib", "true")

    val conf = args.hadoopConf
    conf.set("io.map.index.interval", "1")

    implicit val sparkContext = args.sparkContext("Ingest")

    val accumulo = AccumuloInstance(args.instance, args.zookeeper, args.user, new PasswordToken(args.password))

    implicit val tiler =
      new Tiler[NetCdfBand, SpaceTimeKey] {
        def getExtent(inKey: NetCdfBand): Extent = inKey.extent
        def createKey(inKey: NetCdfBand, spatialComponent: SpatialKey): SpaceTimeKey = 
          SpaceTimeKey(spatialComponent, inKey.time)
      }

    val ingest = new NetCdfIngest(accumulo.catalog, ZoomedLayoutScheme())

    val inPath = new Path(args.input)
    val source = sparkContext.netCdfRDD(inPath)
    val layer = args.layer
    val destCRS = LatLng // Need to create from parameters

    ingest(source, layer, destCRS)
  }
}

// object NetCDFIngest extends Ingest with Logging {

//   type Source = RDD[(NetCdfBand, Tile)]
//   type Sink = RasterRDD[SpaceTimeKey] => Unit

//   def apply (sc: SparkContext)(layerName: String, source: Source, sink:  Sink, destCRS: CRS, layoutScheme: LayoutScheme = ZoomedLayoutScheme()): Unit = {
//     val reprojected = 
//       source.map { case (band, tile) =>
//         val (reprojectedTile, reprojectedExtent) = tile.reproject(band.extent, band.crs, destCRS)
//         band.copy(crs = destCRS, extent = reprojectedExtent) -> reprojectedTile
//       }

//     // First step is to build metaData
//     val metaData: LayerMetaData = {
//       // Note: dimensions would have changed from the stored if we actually reprojected
//       val (band, dims, cellType) =
//         reprojected.map {
//           case (band, tile) => (band, tile.dimensions, tile.cellType)
//         }.first // HERE we make an assumption that all bands share the same extent, therefor the first will do

//       val layerLevel: LayoutLevel = layoutScheme.levelFor(band.extent, CellSize(band.extent, dims))
//       LayerMetaData(
//         LayerId(layerName, layerLevel.zoom),
//         RasterMetaData(cellType, band.extent, band.crs, layerLevel.tileLayout)
//       )
//     }

//     val retiled = 
//       reprojected
//         .retile[SpaceTimeKey](metaData.rasterMetaData)(_.extent)((band, tileId) => SpaceTimeKey(tileId, band.time))

//     val raster = new RasterRDD(retiled, metaData.rasterMetaData)

//     sink(raster)
//   }
// }
