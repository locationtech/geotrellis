package geotrellis.data

import java.awt.image.BufferedImage
import java.awt.image.RenderedImage
import java.io.{File, FileInputStream, FileOutputStream}
import java.nio.ByteBuffer
import java.nio.channels.FileChannel.MapMode._
import javax.imageio.ImageIO
import java.awt.image.WritableRaster
import java.awt.image.Raster
import java.awt.Point

import org.geotools.coverage.grid.GridCoverageFactory
import org.geotools.coverage.grid.GridCoordinates2D
import org.geotools.coverage.grid.io.imageio.geotiff.GeoTiffIIOMetadataDecoder
import org.geotools.coverage.grid.io.imageio.IIOMetadataDumper
import org.geotools.factory.Hints
import org.geotools.gce
import org.geotools.geometry.Envelope2D
import org.geotools.referencing.CRS
import org.geotools.coverage.Category
import org.geotools.coverage.GridSampleDimension
import org.geotools.coverage.GeophysicsCategory

import scala.math.{abs, min, max, round}

import geotrellis._
import geotrellis.process._
import geotrellis.util._


final class GeoTiffReadState(path:String,
                             val layer:RasterLayer,
                             val target:RasterExtent) extends ReadState {
  private var noData:Int = 0
  private var ints:Array[Int] = null

  private def getReader = {
    val fh    = new File(path)
    val gtf   = new gce.geotiff.GeoTiffFormat
    val hints = new Hints(Hints.DEFAULT_COORDINATE_REFERENCE_SYSTEM, CRS.decode("EPSG:3785"))

    if (!fh.canRead) sys.error("can't read %s".format(path))

    gtf.getReader(fh, hints)
  }

  private def initializeNoData(reader:gce.geotiff.GeoTiffReader) = {
    val z = reader.getMetadata.getNoData.toInt
    val bits = reader.read(null).getRenderedImage.getSampleModel.getSampleSize(0)
    noData = if (z < 0) { z + (1 << bits) } else { z }
  }

  def getNoDataValue = noData

  def initSource(pos:Int, size:Int) {
    val x = 0
    val y = pos / layer.rasterExtent.cols
    val w = layer.rasterExtent.cols
    val h = size / layer.rasterExtent.cols

    ints = Array.fill(w * h)(noData)

    val reader = getReader
    initializeNoData(reader)

    val data = reader.read(null).getRenderedImage.getData
    data.getPixels(x, y, w, h, ints)
  }

  @inline
  def assignFromSource(sourceIndex:Int, dest:Array[Int], destIndex:Int) {
    dest(destIndex) = ints(sourceIndex)
  }

  def loadRasterExtent() = {
    val cov = getReader.read(null)

    val env  = cov.getEnvelope2D
    val e = Extent(env.getMinX, env.getMinY, env.getMaxX, env.getMaxY)

    val data = cov.getRenderedImage.getData
    val rows = data.getHeight
    val cols = data.getWidth

    val cellwidth  = (e.xmax - e.xmin) / cols
    val cellheight = (e.ymax - e.ymin) / rows

    RasterExtent(e, cellwidth, cellheight, cols, rows)
  }
}

object GeoTiffReader extends FileReader {
  def readStateFromPath(path:String, rl:RasterLayer, re:RasterExtent) = {
    new GeoTiffReadState(path, rl, re)
  }

  def readStateFromCache(b:Array[Byte], rl:RasterLayer, re:RasterExtent) = {
    sys.error("caching geotif not supported")
  }

  override def readMetadata(path:String) = {
    val state = new GeoTiffReadState(path, null, null)
    val (base, typ) = Filesystem.split(path)
    RasterLayer("", typ, "", base, state.loadRasterExtent(), 3857, 0.0, 0.0)
  }
}

/**
 * This GeoTiffWriter is deprecated.
 *
 * See geotrellis.data.geotiff.Encoder for the preferred approach to
 * encoding rasters to geotiff files.
 */
object GeoTiffWriter extends Writer {
  def rasterType = "geotiff" 
  def dataType = ""

  import geotrellis.data.geotiff._

  def write(path:String, raster:IntRaster, name:String) {
    Encoder.writePath(path, raster, Settings(IntSample, Signed))
  }
}
