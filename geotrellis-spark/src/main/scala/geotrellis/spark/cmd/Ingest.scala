package geotrellis.spark.cmd
import geotrellis._
import geotrellis.data.GeoTiff
import geotrellis.raster.RasterData
import geotrellis.spark.formats.ArgWritable
import geotrellis.spark.formats.TileIdWritable
import geotrellis.spark.metadata.PyramidMetadata
import geotrellis.spark.rdd.RasterSplitGenerator
import geotrellis.spark.rdd.TileIdPartitioner
import geotrellis.spark.tiling.Bounds
import geotrellis.spark.tiling.PixelBounds
import geotrellis.spark.tiling.TileBounds
import geotrellis.spark.tiling.TmsTiling
import geotrellis.spark.utils.HdfsUtils
import geotrellis.spark.utils.SparkUtils

import org.apache.hadoop.fs.Path
import org.apache.hadoop.io.MapFile
import org.apache.hadoop.io.SequenceFile
import org.apache.spark.Logging
import org.geotools.coverage.grid.GeneralGridEnvelope
import org.geotools.coverage.grid.GridCoverage2D
import org.geotools.coverage.grid.GridGeometry2D
import org.geotools.coverage.processing.Operations
import org.geotools.geometry.GeneralEnvelope
import org.opengis.coverage.grid.GridCoverage
import org.opengis.geometry.Envelope

import java.awt.Rectangle
import java.awt.image.DataBufferByte
import java.awt.image.DataBufferDouble
import java.awt.image.DataBufferFloat
import java.awt.image.DataBufferInt
import java.awt.image.DataBufferShort
import java.net.URL

import com.quantifind.sumac.ArgMain
import javax.media.jai.Interpolation

/**
 * @author akini
 * 
 * Ingest GeoTIFFs into ArgWritable.  
 *
 * Ingest --input <path-to-tiffs> --output <path-to-raster> --sparkMaster <spark-master-ip>
 * 
 * e.g., Ingest --input file:///home/akini/test/small_files/all-ones.tif --output file:///tmp/all-ones 
 * 
 * Constraints:
 * 
 * --input <path-to-tiffs> - this can either be a directory or a single tiff file and has to be on the local fs.
 * Currently, mosaicing is not implemented so only the single tiff file case is tested 
 * 
 * --output <path-to-raster> - this can be either on hdfs (hdfs://) or local fs (file://). If the directory
 * already exists, it is deleted
 * 
 * 
 * Outstanding issues:
 * 1. Saving to multiple partitions 
 * 2. Mosaicing overlapping tiles 
 * 
 * These are features more than issues
 * 1. Faster local ingest using .par 
 * 2. Faster local ingest using spark api
 */
object Ingest extends ArgMain[CommandArguments] with Logging {

  val Default_Projection = "EPSG:4326"
  System.setProperty("com.sun.media.jai.disableMediaLib", "true")

  def main(args: CommandArguments) {
    val inPath = new Path(args.input)
    val outPath = new Path(args.output)

    val conf = SparkUtils.createHadoopConfiguration

    val (files, meta) = PyramidMetadata.fromTifFiles(inPath, conf)
    println("------- FILES ------")
    println(files.mkString("\n"))
    println("\n\n\n")
    println("------- META ------")
    println(meta)

    val tiles = files.flatMap(file => tiffToTiles(file, meta))

    logInfo(s"Deleting and creating output path: $outPath")
    val outFs = outPath.getFileSystem(conf)
    outFs.delete(outPath, true)
    outFs.mkdirs(outPath)

    logInfo("Saving metadata: ")
    logInfo(meta.toString)
    meta.save(outPath, conf)

    val outPathWithZoom = new Path(outPath, meta.maxZoomLevel.toString)
    logInfo(s"Creating Output Path With Zoom: $outPathWithZoom")
    outFs.mkdirs(outPathWithZoom)

    val tileBounds = meta.rasterMetadata(meta.maxZoomLevel.toString).tileBounds
    val (zoom, tileSize, rasterType) = (meta.maxZoomLevel, meta.tileSize, meta.rasterType)
    val splitGenerator = RasterSplitGenerator(tileBounds, zoom,
      TmsTiling.tileSizeBytes(tileSize, rasterType),
      HdfsUtils.blockSize(conf))

    val partitioner = TileIdPartitioner(splitGenerator, outPathWithZoom, conf)

    //val partitioner = TileIdPartitioner(SplitGenerator.EMPTY, outPathWithZoom, conf)
    logInfo("Saving splits: " + partitioner)

    val mapFilePath = new Path(outPathWithZoom, "part-00000")
    val key = new TileIdWritable()

    logInfo(s"Saving ${tiles.length} tiles to $mapFilePath")
    val writer = new MapFile.Writer(conf, outFs, mapFilePath.toUri.toString,
      classOf[TileIdWritable], classOf[ArgWritable],
      SequenceFile.CompressionType.RECORD)
    try {
      tiles.foreach {
        case (tileId, tile) => {
          key.set(tileId)
          writer.append(key, ArgWritable.fromRasterData(tile))
        }
      }
    } finally {
      writer.close
    }
    logInfo("Done saving tiles")

  }

  private def tiffToTiles(file: Path, pyMeta: PyramidMetadata): List[(Long, RasterData)] = {
    val url = new URL(file.toUri().toString())
    val image = GeoTiff.getGridCoverage2D(url)
    val imageMeta = GeoTiff.getMetadata(url).get
    val (zoom, tileSize, rasterType, nodata) =
      (pyMeta.maxZoomLevel, pyMeta.tileSize, pyMeta.rasterType, pyMeta.nodata)
    val tb = getTileBounds(imageMeta.bounds, zoom, tileSize)

    val tileIds = for {
      ty <- tb.s to tb.n
      tx <- tb.w to tb.e
      tileId = TmsTiling.tileId(tx, ty, zoom)
      bounds = TmsTiling.tileToBounds(tx, ty, zoom, tileSize)
      tile = cutTile(image, bounds, pyMeta)
    } yield (tileId, tile)

    tileIds.toList
  }

  private def cutTile(image: GridCoverage, bounds: Bounds, pyMeta: PyramidMetadata): RasterData = {
    val (zoom, tileSize, rasterType, nodata) =
      (pyMeta.maxZoomLevel, pyMeta.tileSize, pyMeta.rasterType, pyMeta.nodata)

    val tileEnvelope = new GeneralEnvelope(Array(bounds.w, bounds.s), Array(bounds.e, bounds.n))
    tileEnvelope.setCoordinateReferenceSystem(image.getCoordinateReferenceSystem())

    val nodataArr = Array(nodata)
    val tileGeometry = new GridGeometry2D(new GeneralGridEnvelope(new Rectangle(
      0, 0, tileSize, tileSize)), tileEnvelope)

    val tile = Operations.DEFAULT.resample(image, null, tileGeometry,
      Interpolation.getInstance(Interpolation.INTERP_NEAREST), nodataArr)
      .asInstanceOf[GridCoverage2D]

    //val (h, w) = (tile.getRenderedImage().getData().getHeight(), tile.getRenderedImage().getData().getWidth())
    val rawDataBuff = tile.getRenderedImage().getData().getDataBuffer()
    //println(s"h=$h,w=$w,dataBuffLen=${rawDataBuff.asInstanceOf[DataBufferFloat].getData().length}, " + 
    //"bounds=${bounds} and env=${tileEnvelope}")

    val rd = rasterType match {
      case TypeDouble => RasterData(rawDataBuff.asInstanceOf[DataBufferDouble].getData(), tileSize, tileSize)
      case TypeFloat  => RasterData(rawDataBuff.asInstanceOf[DataBufferFloat].getData(), tileSize, tileSize)
      case TypeInt    => RasterData(rawDataBuff.asInstanceOf[DataBufferInt].getData(), tileSize, tileSize)
      case TypeShort  => RasterData(rawDataBuff.asInstanceOf[DataBufferShort].getData(), tileSize, tileSize)
      case TypeByte   => RasterData(rawDataBuff.asInstanceOf[DataBufferByte].getData(), tileSize, tileSize)
      case _          => sys.error("Unrecognized AWT type - " + rasterType)
    }
    NoDataHandler.removeUserNodata(rd, nodata)
  }

  /* The following methods are here vs. TmsTiling as they convert Envelope to TmsTiling types
   * and there isn't a need to expose Envelope to the rest of geotrellis/geotrellis-spark
   */
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

  /* debugging only */
  private def inspect(tile: GridCoverage2D): Unit = {
    val env = tile.getEnvelope()
    val r = tile.getRenderedImage().getData()
    for {
      py <- 0 until r.getHeight()
      if (py < 10)
      px <- 0 until r.getWidth()
      if (px < 10)
    } {
      val d = r.getSampleFloat(px, py, 0)
      println(s"($px,$py)=$d")
    }
  }
}