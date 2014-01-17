package geotrellis.spark.ingest

import geotrellis.data.GeoTiff
import geotrellis.raster.RasterData
import geotrellis.spark.metadata.JacksonWrapper
import geotrellis.spark.metadata.PyramidMetadata
import geotrellis.spark.tiling.Bounds
import geotrellis.spark.tiling.TmsTiling
import org.apache.hadoop.fs.Path
import org.geotools.coverage.grid.GridCoverage2D
import org.geotools.coverage.processing.Operations
import org.geotools.geometry.GeneralEnvelope
import org.opengis.coverage.grid.GridCoverage
import java.net.URL
import geotrellis.spark.utils.SparkUtils

object Ingest {
val Default_Projection = "EPSG:4326"
  System.setProperty("com.sun.media.jai.disableMediaLib", "true")

  private def getPixelBounds(envelope: GeneralEnvelope, zoom: Int, tileSize: Int) = {
    val (w, s, e, n) =
      (envelope.getLowerCorner.getOrdinate(0),
        envelope.getLowerCorner.getOrdinate(1),
        envelope.getUpperCorner.getOrdinate(0),
        envelope.getUpperCorner.getOrdinate(1))

    val bounds = new Bounds(w, s, e, n)
    val tileBounds = TmsTiling.boundsToTile(bounds, zoom, tileSize)
    val (pixelLower, pixelUpper) = 
      (TmsTiling.latLonToPixels(s, w, zoom, tileSize),
      TmsTiling.latLonToPixels(n, e, zoom, tileSize))
    (pixelLower, pixelUpper)
  }
  
  def cutTile(image: GridCoverage2D, croppedEnvelope: GeneralEnvelope): GridCoverage = {
    val cropped = Operations.DEFAULT.crop(image, croppedEnvelope).asInstanceOf[GridCoverage]
    cropped
    //val scaled = Operations.DEFAULT.scale(cropped, 0.5, 0.5, 0, 0, null)
    //scaled
  }

  // takes a tile and gives back a Raster for that tile
//  def toRaster(tile: GridCoverage, imageMeta: Metadata, envelope: GeneralEnvelope): Raster = {
//    val rawDataBuff = tile.getRenderedImage().getData().getDataBuffer()
//    val dataBuff = imageMeta.rasterType match {
//      case DataBuffer.TYPE_FLOAT => RasterData(rawDataBuff.asInstanceOf[DataBufferFloat].getData())
//      case DataBuffer.TYPE_INT   => rawDataBuff.asInstanceOf[DataBufferInt]
//
//    }
//  }
  
  def tiffToTiles(file: Path, meta: PyramidMetadata): List[(Long,RasterData)] = {
    val url = new URL(file.toUri().toString())
    val metadata = GeoTiff.getMetadata(url).get
    println(metadata)
     val resampled = cutTile(GeoTiff.getGridCoverage2D(url), metadata.bounds)
    println(resampled.getEnvelope())
    List((1, RasterData(Array(1,0,1,0),2,2)))
  }
  def main(args: Array[String]) {
    val inPath = new Path("file:///home/akini/test/small_files/all-ones.tif")
    val conf = SparkUtils.createHadoopConfiguration

    val (files, meta) = PyramidMetadata.fromTifFiles(inPath, conf)
    println("------- FILES ------")
    println(files.mkString("\n"))
    println("\n\n\n")
    println("------- META ------")
    println(JacksonWrapper.prettyPrint(meta))
    
    files.flatMap(file => tiffToTiles(file,meta)).foreach(t => println(s"tileId=${t._1} and tile length=${t._2.length}"))
  }
}