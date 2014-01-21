package geotrellis.spark.ingest

import geotrellis.Extent
import geotrellis.Raster
import geotrellis.RasterExtent
import geotrellis.RasterType
import geotrellis.TypeFloat
import geotrellis.TypeInt
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
import geotrellis.spark.utils.HdfsUtils
import geotrellis.spark.utils.SparkUtils
import org.apache.hadoop.fs.Path
import org.apache.hadoop.io.MapFile
import org.apache.hadoop.io.SequenceFile
import org.geotools.coverage.processing.Operations
import org.geotools.geometry.GeneralEnvelope
import org.opengis.coverage.grid.GridCoverage
import org.opengis.geometry.Envelope
import java.awt.image.DataBufferFloat
import java.awt.image.DataBufferInt
import java.net.URL
import com.quantifind.sumac.ArgMain
import com.quantifind.sumac.FieldArgs
import org.geotools.coverage.grid.GridCoverage2D
import org.geotools.coverage.grid.GridGeometry2D
import org.geotools.coverage.grid.GeneralGridEnvelope
import java.awt.Rectangle
import javax.media.jai.Interpolation
import geotrellis.data.GeoTiffWriter

class Arguments extends FieldArgs {
  var input: String = _
  var output: String = _
}

/*
 * Outstanding issues:
 * 1. See PyramidMetadata's outstanding issues 
 * 2. See TODO in here (replacing nodata value with NaN, etc.)  
 * 3. Saving to multiple partitions 
 * 4. Mosaicing overlapping tiles 
 * 
 * These are features more than issues
 * 1. Faster local ingest using .par 
 * 2. Faster local ingest using spark api
 */
object Ingest extends ArgMain[Arguments] {

  val Default_Projection = "EPSG:4326"
  System.setProperty("com.sun.media.jai.disableMediaLib", "true")

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
      GeoTiffWriter.write(s"/tmp/all-ones-ingested/tile-${t._1}.tif", t._2, "float")
    })*/

    println("Deleting and creating output path: " + outPath)
    val outFs = outPath.getFileSystem(conf)
    outFs.delete(outPath, true)
    outFs.mkdirs(outPath)

    println("Saving metadata: ")
    meta.save(outPath, conf)

    val outPathWithZoom = new Path(outPath, meta.maxZoomLevel.toString)
    println("Creating Output Path With Zoom: " + outPathWithZoom)
    outFs.mkdirs(outPathWithZoom)

    val tileBounds = meta.rasterMetadata(meta.maxZoomLevel.toString).tileBounds
    val (zoom, tileSize, rasterType) = (meta.maxZoomLevel, meta.tileSize, meta.rasterType)
    val splitGenerator = RasterSplitGenerator(tileBounds, zoom,
      TmsTiling.tileSizeBytes(tileSize, rasterType),
      HdfsUtils.blockSize(conf))

    val partitioner = TileIdPartitioner(splitGenerator, outPathWithZoom, conf)

    //val partitioner = TileIdPartitioner(SplitGenerator.EMPTY, outPathWithZoom, conf)
    println("Saving splits: " + partitioner)

    val mapFilePath = new Path(outPathWithZoom, "part-00000")
    val key = new TileIdWritable()

    println(s"Saving ${tiles.length} tiles to $mapFilePath")
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
    println("Done saving tiles")

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

  // takes an image and a bounds and gives back a tile corresponding to those bounds
  private def cutTile(image: GridCoverage, bounds: Bounds, pyMeta: PyramidMetadata): RasterData = {
    val (zoom, tileSize, rasterType, nodata) =
      (pyMeta.maxZoomLevel, pyMeta.tileSize, pyMeta.rasterType, pyMeta.nodata)

    val tileEnvelope = new GeneralEnvelope(Array(bounds.w, bounds.s), Array(bounds.e, bounds.n))
    tileEnvelope.setCoordinateReferenceSystem(image.getCoordinateReferenceSystem())

    // TODO - translate nodata to NaN
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

    // TODO - handle other types
    val rd = rasterType match {
      case TypeFloat => RasterData(rawDataBuff.asInstanceOf[DataBufferFloat].getData(), tileSize, tileSize)
      case TypeInt   => RasterData(rawDataBuff.asInstanceOf[DataBufferInt].getData(), tileSize, tileSize)
    }
    //Raster(rd, RasterExtent(Extent(bounds.w, bounds.s, bounds.e, bounds.n), tileSize, tileSize))
    rd
  }

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