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
                          val rasterExtent:RasterExtent,
                          val target:RasterExtent,
                          val typ:RasterType,
                          val reader:gce.geotiff.GeoTiffReader) extends ReadState {
  def getType = typ

  private var noData:Int = NODATA
  private var data:Array[Int] = null

  private def initializeNoData(reader:gce.geotiff.GeoTiffReader) = 
    noData = reader.getMetadata.getNoData.toInt

  def initSource(pos:Int, size:Int) {
    val x = 0
    val y = pos / rasterExtent.cols
    val w = rasterExtent.cols
    val h = size / rasterExtent.cols

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
                          val rasterExtent:RasterExtent,
                          val target:RasterExtent,
                          val typ:RasterType,
                          val reader:gce.geotiff.GeoTiffReader) extends ReadState {
  def getType = typ

  private var noData:Double = 0.0
  private var data:Array[Double] = null
  
  private def initializeNoData(reader:gce.geotiff.GeoTiffReader) = {
    noData = reader.getMetadata.getNoData.toDouble
  }

  def getNoDataValue = noData

  def initSource(pos:Int, size:Int) {
    val x = 0
    val y = pos / rasterExtent.cols
    val w = rasterExtent.cols
    val h = size / rasterExtent.cols

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

class GeoTiffReader(path:String) extends FileReader(path) {
  private def getReader() = {
    val fh    = new File(path)
    val gtf   = new gce.geotiff.GeoTiffFormat
    val hints = new Hints(Hints.DEFAULT_COORDINATE_REFERENCE_SYSTEM, CRS.decode("EPSG:3785"))

    if (!fh.canRead) sys.error("can't read %s".format(path))

    gtf.getReader(fh, hints)
  }

  def loadRasterExtent(reader:Option[gce.geotiff.GeoTiffReader] = None) = {
    val cov = reader match {
      case Some(r) => r.read(null)
      case None => getReader().read(null)
    }

    val env  = cov.getEnvelope2D
    val e = Extent(env.getMinX, env.getMinY, env.getMaxX, env.getMaxY)

    val data = cov.getRenderedImage.getData
    val rows = data.getHeight
    val cols = data.getWidth

    val cellwidth  = (e.xmax - e.xmin) / cols
    val cellheight = (e.ymax - e.ymin) / rows

    RasterExtent(e, cellwidth, cellheight, cols, rows)
  }

  def readStateFromPath(rasterType:RasterType, 
                        rasterExtent:RasterExtent,targetExtent:RasterExtent) = {
    val reader = getReader()
    val dt = reader.read(null).getRenderedImage.getSampleModel.getDataType

    val ore = rasterExtent
    dt match {
      // Bytes are unsigned in GTiff
      case TYPE_BYTE    =>   new GeoTiffIntReadState(path, ore, targetExtent, TypeShort, reader) 
      case TYPE_SHORT   =>   new GeoTiffIntReadState(path, ore, targetExtent, TypeShort, reader)
      case TYPE_USHORT  =>   new GeoTiffIntReadState(path, ore, targetExtent, TypeInt, reader)
      case TYPE_INT     =>   new GeoTiffIntReadState(path, ore, targetExtent, TypeInt, reader)
      case TYPE_FLOAT   =>   new GeoTiffDoubleReadState(path, ore, targetExtent, TypeFloat, reader)
      case TYPE_DOUBLE  =>   new GeoTiffDoubleReadState(path, ore, targetExtent, TypeDouble, reader)
      case _            =>   new GeoTiffDoubleReadState(path, ore, targetExtent, TypeDouble, reader)
    }
  }

  def readStateFromCache(b:Array[Byte], rl:RasterLayer, re:RasterExtent) = {
    sys.error("caching geotif not supported")
  }

  override def readMetadata() = {
    val extent = loadRasterExtent()
    val info = new RasterLayerInfo("", TypeDouble, extent, 3857, 0.0, 0.0)
    Some(new GeoTiffRasterLayer(info,path,None))
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
