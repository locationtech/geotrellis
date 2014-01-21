package geotrellis.spark.ingest

import geotrellis.Extent
import geotrellis.Raster
import geotrellis.RasterExtent
import geotrellis.data.GeoTiff
import geotrellis.raster.RasterData
import geotrellis.spark.formats.ArgWritable
import geotrellis.spark.formats.TileIdWritable
import geotrellis.spark.metadata.JacksonWrapper
import geotrellis.spark.metadata.PyramidMetadata
import geotrellis.spark.rdd.RasterSplitGenerator
import geotrellis.spark.rdd.TileIdPartitioner
import geotrellis.spark.tiling.Bounds
import geotrellis.spark.tiling.PixelBounds
import geotrellis.spark.tiling.TileBounds
import geotrellis.spark.tiling.TmsTiling
import geotrellis.spark.utils.SparkUtils

import org.apache.hadoop.fs.Path
import org.apache.hadoop.io.MapFile
import org.apache.hadoop.io.SequenceFile
import org.geotools.coverage.processing.Operations
import org.geotools.geometry.GeneralEnvelope
import org.opengis.coverage.grid.GridCoverage
import org.opengis.geometry.Envelope

import java.awt.image.DataBuffer
import java.awt.image.DataBufferFloat
import java.awt.image.DataBufferInt
import java.net.URL

import com.quantifind.sumac.ArgMain
import com.quantifind.sumac.FieldArgs

class Arguments extends FieldArgs {
  var input: String = _
  var output: String = _
}

object Ingest extends ArgMain[Arguments] {

  val Default_Projection = "EPSG:4326"
  System.setProperty("com.sun.media.jai.disableMediaLib", "true")

  def tiffToTiles(file: Path, pyMeta: PyramidMetadata): List[(Long, Raster)] = {
    val url = new URL(file.toUri().toString())
    val img = GeoTiff.getGridCoverage2D(url)
    val imgMeta = GeoTiff.getMetadata(url).get
    println(imgMeta)
    val (zoom, tileSize, rasterType) = (pyMeta.maxZoomLevel, pyMeta.tileSize, pyMeta.rasterType)
    val tb = getTileBounds(imgMeta.bounds, zoom, tileSize)

    val tileIds = for (
      ty <- tb.s to tb.n;
      tx <- tb.w to tb.e;
      tileId = TmsTiling.tileId(tx, ty, zoom);
      bounds = TmsTiling.tileToBounds(tx, ty, zoom, tileSize);
      tile = cutTile(img, bounds, rasterType, zoom, tileSize)
    ) yield (tileId, tile)

    println(tileIds)
    tileIds.toList
  }

  // given 
  private def getTileBounds(env: Envelope, zoom: Int, tileSize: Int): TileBounds = {
    val bounds = getBounds(env)
    TmsTiling.boundsToTile(bounds, zoom, tileSize)
  }

  private def getBounds(env: Envelope): Bounds = {
    val (w, s, e, n) =
      (env.getLowerCorner.getOrdinate(0),
        env.getLowerCorner.getOrdinate(1),
        env.getUpperCorner.getOrdinate(0),
        env.getUpperCorner.getOrdinate(1))
    Bounds(w, s, e, n)
  }
  private def getPixelBounds(env: Envelope, zoom: Int, tileSize: Int) = {
    val bounds = getBounds(env)
    val tileBounds = TmsTiling.boundsToTile(bounds, zoom, tileSize)
    val (pixelLower, pixelUpper) =
      (TmsTiling.latLonToPixels(bounds.s, bounds.w, zoom, tileSize),
        TmsTiling.latLonToPixels(bounds.n, bounds.e, zoom, tileSize))
    new PixelBounds(0, 0,
      pixelUpper.px - pixelLower.px, pixelUpper.py - pixelLower.py)
  }

  /*private def cutTile(image: GridCoverage2D, croppedEnvelope: GeneralEnvelope): GridCoverage = {
    println(s"before cropping, dataBuffLen=${image.getRenderedImage().getData().getDataBuffer().getSize}")
    val cropped = Operations.DEFAULT.crop(image, croppedEnvelope).asInstanceOf[GridCoverage]
    cropped
    //val scaled = Operations.DEFAULT.scale(cropped, 0.5, 0.5, 0, 0, null)
    //scaled
  }*/

  // takes a tile and gives back a Raster for that tile
  private def cutTile(image: GridCoverage, bounds: Bounds, rasterType: Int, zoom: Int, tileSize: Int): Raster = {
    val env = new GeneralEnvelope(Array(bounds.w, bounds.s), Array(bounds.e, bounds.n))
    env.setCoordinateReferenceSystem(image.getCoordinateReferenceSystem())

    val cropImage = Operations.DEFAULT.crop(image, env).asInstanceOf[GridCoverage]

    // TODO - assert that env corresponds to tileSizextileSize    
    val rawDataBuff = cropImage.getRenderedImage().getData().getDataBuffer()
    println(s"dataBuffLen=${rawDataBuff.asInstanceOf[DataBufferFloat].getData().length}, bounds=${bounds} and env=${env}")
    val rd = rasterType match {
      case DataBuffer.TYPE_FLOAT => RasterData(rawDataBuff.asInstanceOf[DataBufferFloat].getData(), tileSize, tileSize)
      case DataBuffer.TYPE_INT   => RasterData(rawDataBuff.asInstanceOf[DataBufferInt].getData(), tileSize, tileSize)

    }
    Raster(rd, RasterExtent(Extent(bounds.w, bounds.s, bounds.e, bounds.n), tileSize, tileSize))
  }

  def main(args: Arguments) {
    val inPath = new Path(args.input)
    val outPath = new Path(args.output)

    val conf = SparkUtils.createHadoopConfiguration

    val (files, meta) = PyramidMetadata.fromTifFiles(inPath, conf)
    println("------- FILES ------")
    println(files.mkString("\n"))
    println("\n\n\n")
    println("------- META ------")
    println(JacksonWrapper.prettyPrint(meta))

    val tiles = files.flatMap(file => tiffToTiles(file, meta))
    /*tiles.foreach(t => {
      println(s"tileId=${t._1} and tile length=${t._2.length}")
      GeoTiffWriter.write(s"/tmp/all-ones-ingested/tile-${t._1}.tif", t._2, "float")
    })*/

    println("Deleting and creating output path: " + outPath)
    val outFs = outPath.getFileSystem(conf)
    println(outFs)
    outFs.delete(outPath, true)
    outFs.mkdirs(outPath)

    println("Saving metadata: ")
    meta.save(outPath, conf)

    val outPathWithZoom = new Path(outPath, meta.maxZoomLevel.toString)
    println("Creating Output Path With Zoom: " + outPathWithZoom)
    outFs.mkdirs(outPathWithZoom)

    val tb = meta.rasterMetadata(meta.maxZoomLevel.toString).tileBounds
    val (zoom, tileSize) = (meta.maxZoomLevel, meta.tileSize)
    val tileSizeBytes = 512 * 512 * 4
    val blockSizeBytes = 67108864
    val tp = TileIdPartitioner(RasterSplitGenerator(tb, zoom, tileSizeBytes, blockSizeBytes), outPathWithZoom, conf)
    //val tp = TileIdPartitioner(SplitGenerator.EMPTY, outPathWithZoom, conf)
    println("Saving splits: " + tp)

    val mapFilePath = new Path(outPathWithZoom, "part-00000")
    val key = new TileIdWritable()

    println(s"Saving ${tiles.length} tiles to $mapFilePath")
    val writer = new MapFile.Writer(conf, outFs, mapFilePath.toUri.toString,
      classOf[TileIdWritable], classOf[ArgWritable],
      SequenceFile.CompressionType.RECORD)
    try {
      tiles.foreach {
        case (tileId, raster) => {
          key.set(tileId)
          val value = ArgWritable.fromRasterData(raster.toArrayRaster.data)
          writer.append(key, value)

        }
      }
    } finally {
      writer.close
    }
    println("Done saving tiles")

  }
}