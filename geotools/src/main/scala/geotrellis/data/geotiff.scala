package geotrellis.data

import java.awt.image.BufferedImage
import java.awt.image.RenderedImage
import java.io.{File, FileInputStream, FileOutputStream}
import java.nio.ByteBuffer
import java.nio.channels.FileChannel.MapMode._
import javax.imageio.ImageIO
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
import geotrellis.util.Filesystem
import java.awt.image.DataBuffer._


class GeoTiffIntReadState(path:String,
                          val layer:RasterLayer,
                          val target:RasterExtent,
                          val typ:RasterType,
                          val reader:gce.geotiff.GeoTiffReader) extends ReadState {
  def getType = typ

  private var noData:Int = NODATA
  private var data:Array[Int] = null

  private def initializeNoData(reader:gce.geotiff.GeoTiffReader) = {
    noData = reader.getMetadata.getNoData.toInt
    // val bits = reader.read(null).getRenderedImage.getSampleModel.getSampleSize(0)
    // noData = if (z < 0) { z + (1 << bits) } else { z }
  }

  def initSource(pos:Int, size:Int) {
    val x = 0
    val y = pos / layer.rasterExtent.cols
    val w = layer.rasterExtent.cols
    val h = size / layer.rasterExtent.cols

    initializeNoData(reader)
    data = Array.fill(w * h)(noData)

    val geoRaster = reader.read(null).getRenderedImage.getData
    geoRaster.getPixels(x, y, w, h, data)
  }

  @inline
  def assignFromSource(sourceIndex:Int, dest:MutableRasterData, destIndex:Int) {
    dest(destIndex) = data(sourceIndex)
  }

  protected[this] override def translate(rData:MutableRasterData) {
    if(noData != NODATA) {
      println(s"NoData value is $noData, converting to Int.MinValue")
      var i = 0
      val len = rData.length
      var conflicts = 0
      while (i < len) {
        if(rData(i) == NODATA) conflicts += 1
        if (rData(i) == noData) rData.updateDouble(i, NODATA)
        i += 1
      }
      if(conflicts > 0) {
        println(s"[WARNING]  GeoTiff file $path contained values of ${NODATA}, which are considered to be NO DATA values in ARG format. There are $conflicts raster cells that are now considered NO DATA values in the converted format.")
      }
    }
  }
}

class GeoTiffDoubleReadState(path:String,
                          val layer:RasterLayer,
                          val target:RasterExtent,
                          val typ:RasterType,
                          val reader:gce.geotiff.GeoTiffReader) extends ReadState {
  def getType = typ

  private var noData:Double = 0.0
  private var data:Array[Double] = null
  
  private def initializeNoData(reader:gce.geotiff.GeoTiffReader) = {
    noData = reader.getMetadata.getNoData.toDouble
//    val bits = reader.read(null).getRenderedImage.getSampleModel.getSampleSize(0)
//    noData = if (z < 0) { z + (1 << bits) } else { z }
  }

  def getNoDataValue = noData

  def initSource(pos:Int, size:Int) {
    val x = 0
    val y = pos / layer.rasterExtent.cols
    val w = layer.rasterExtent.cols
    val h = size / layer.rasterExtent.cols

    initializeNoData(reader)
    data = Array.fill(w * h)(noData)
    val geoRaster = reader.read(null).getRenderedImage.getData
    geoRaster.getPixels(x, y, w, h, data)
  }

  @inline
  def assignFromSource(sourceIndex:Int, dest:MutableRasterData, destIndex:Int) {
    dest.updateDouble(destIndex, data(sourceIndex))
  }

  protected[this] override def translate(rData:MutableRasterData) {
    if(noData != Double.NaN) {
      println(s"NoData value is $noData, converting to NaN")
      var i = 0
      val len = rData.length
      var conflicts = 0
      while (i < len) {
        if(rData(i) == Double.NaN) conflicts += 1
        if (rData(i) == noData) rData.updateDouble(i, Double.NaN)
        i += 1
      }
      if(conflicts > 0) {
        println(s"[WARNING]  GeoTiff contained values of ${Double.NaN}, which are considered to be NO DATA values in ARG format. There are $conflicts raster cells that are now considered NO DATA values in the converted format.")
      }
    }
  }

}

// final class GeoTiffReadState(path:String,
//                              val layer:RasterLayer,
//                              val target:RasterExtent) extends IntReadState {
//   private var noData:Int = 0
//   private var ints:Array[Int] = null

//   def getType = TypeInt

//   private def getReader = {
//     val fh    = new File(path)
//     val gtf   = new gce.geotiff.GeoTiffFormat
//     val hints = new Hints(Hints.DEFAULT_COORDINATE_REFERENCE_SYSTEM, CRS.decode("EPSG:3785"))

//     if (!fh.canRead) sys.error("can't read %s".format(path))

//     gtf.getReader(fh, hints)
//   }

//   private def initializeNoData(reader:gce.geotiff.GeoTiffReader) = {
//     val z = reader.getMetadata.getNoData.toInt
//     val bits = reader.read(null).getRenderedImage.getSampleModel.getSampleSize(0)
//     noData = if (z < 0) { z + (1 << bits) } else { z }
//     println(s"NODATA VALUE: ($z,$noData)")
//   }

//   def getNoDataValue = noData

//   def initSource(pos:Int, size:Int) {
//     val x = 0
//     val y = pos / layer.rasterExtent.cols
//     val w = layer.rasterExtent.cols
//     val h = size / layer.rasterExtent.cols

//     ints = Array.fill(w * h)(NODATA)

//     val reader = getReader
//     initializeNoData(reader)

//     val data = reader.read(null).getRenderedImage.getData
//     data.getPixels(x, y, w, h, ints)
//   }

//   @inline
//   def assignFromSource(sourceIndex:Int, dest:MutableRasterData, destIndex:Int) {
//     dest(destIndex) = ints(sourceIndex)
//   }

//   def loadRasterExtent() = {
//     val cov = getReader.read(null)

//     val env  = cov.getEnvelope2D
//     val e = Extent(env.getMinX, env.getMinY, env.getMaxX, env.getMaxY)

//     val data = cov.getRenderedImage.getData
//     val rows = data.getHeight
//     val cols = data.getWidth

//     val cellwidth  = (e.xmax - e.xmin) / cols
//     val cellheight = (e.ymax - e.ymin) / rows

//     RasterExtent(e, cellwidth, cellheight, cols, rows)
//   }
// }

object GeoTiffReader extends FileReader {
  private def getReader(path:String) = {
    val fh    = new File(path)
    val gtf   = new gce.geotiff.GeoTiffFormat
    val hints = new Hints(Hints.DEFAULT_COORDINATE_REFERENCE_SYSTEM, CRS.decode("EPSG:3785"))

    if (!fh.canRead) sys.error("can't read %s".format(path))

    gtf.getReader(fh, hints)
  }

  def loadRasterExtent(reader:gce.geotiff.GeoTiffReader) = {
    val cov = reader.read(null)

    val env  = cov.getEnvelope2D
    val e = Extent(env.getMinX, env.getMinY, env.getMaxX, env.getMaxY)

    val data = cov.getRenderedImage.getData
    val rows = data.getHeight
    val cols = data.getWidth

    val cellwidth  = (e.xmax - e.xmin) / cols
    val cellheight = (e.ymax - e.ymin) / rows

    RasterExtent(e, cellwidth, cellheight, cols, rows)
  }

  def readStateFromPath(path:String, rl:RasterLayer, re:RasterExtent) = {
    val reader = getReader(path)
    val dt = reader.read(null).getRenderedImage.getSampleModel.getDataType

    dt match {
      case TYPE_BYTE    =>   new GeoTiffIntReadState(path, rl, re, TypeShort, reader) // Bytes are unsigned in GTiff
      case TYPE_SHORT   =>   new GeoTiffIntReadState(path, rl, re, TypeShort, reader)
      case TYPE_USHORT  =>   new GeoTiffIntReadState(path, rl, re, TypeInt, reader)
      case TYPE_INT     =>   new GeoTiffIntReadState(path, rl, re, TypeInt, reader)
      case TYPE_FLOAT   =>   new GeoTiffDoubleReadState(path, rl, re, TypeFloat, reader)
      case TYPE_DOUBLE  =>   new GeoTiffDoubleReadState(path, rl, re, TypeDouble, reader)
      case _            =>   new GeoTiffDoubleReadState(path, rl, re, TypeDouble, reader)
    }
  }

  def readStateFromCache(b:Array[Byte], rl:RasterLayer, re:RasterExtent) = {
    sys.error("caching geotif not supported")
  }

  override def readMetadata(path:String) = {
    val extent = loadRasterExtent(getReader(path))
    val (base, typ) = Filesystem.split(path)
    RasterLayer("", typ, "", base, extent, 3857, 0.0, 0.0)
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

  def write(path:String, raster:Raster, name:String) {
    val settings = raster.data.getType match {
      case TypeBit | TypeByte => Settings.int8
      case TypeShort => Settings.int16
      case TypeInt => Settings.int32
      case TypeFloat => Settings.float32
      case TypeDouble => Settings.float64
    }
    Encoder.writePath(path, raster, settings)
  }
}
