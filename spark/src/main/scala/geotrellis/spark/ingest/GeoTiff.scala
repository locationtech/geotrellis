package geotrellis.spark.ingest

import geotrellis._

import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.FSDataInputStream
import org.apache.hadoop.fs.Path
import org.apache.spark.Logging
import org.geotools.coverage.grid.GridCoverage2D
import org.geotools.coverage.grid.GridEnvelope2D
import org.geotools.coverage.grid.io.AbstractGridCoverage2DReader
import org.geotools.coverage.grid.io.AbstractGridFormat
import org.geotools.coverage.grid.io.GridFormatFinder
import org.geotools.factory.Hints
import org.geotools.gce.geotiff.{ GeoTiffReader => GTGeoTiffReader }
import org.geotools.geometry.GeneralEnvelope
import org.geotools.referencing.CRS

import java.awt.image.DataBuffer._

import scala.collection.JavaConversions._
import scala.collection.mutable.Set

/*
 * The spark code base uses this GeoTiff instead of geotrellis.data.GeoTiff in geotools project, 
 * as this handles streams out of HDFS whereas the other is limited to files in the local FS. 
 * As such, this takes in HDFS Paths instead of URL. 
 *  
 * This should be replaced by the native Geotiff reader once that is completed.
 */
object GeoTiff extends Logging {

  final val DefaultProjection = "EPSG:4326"

  private lazy val formats = {
    HdfsImageInputStreamSpi.register
    loadFormats
  }

  private val hints = new Hints(Hints.DEFAULT_COORDINATE_REFERENCE_SYSTEM, CRS.decode(DefaultProjection))

  case class Metadata(
    extent: Extent,
    pixelSize: (Double, Double),
    pixels: (Int, Int),
    bands: Int,
    rasterType: Int,
    nodata: Double) {

    private def isPixelSizeEqual(l: (Double, Double), r: (Double, Double)) =
      (l._1 - r._1).abs < 0.0001 && (l._2 - r._2).abs < 0.0001

    def merge(other: Metadata): Metadata = {
      if (bands != other.bands)
        sys.error(s"Error: All input tifs must have the same number of bands ${bands} != ${other.bands}")
      if (!isPixelSizeEqual(pixelSize, other.pixelSize))
        sys.error(s"Error: All input tifs must have the same resolution ${pixelSize} != ${other.pixelSize}")
      if (rasterType != other.rasterType)
        sys.error(s"Error: All input tifs must have same raster type ${rasterType} != ${other.rasterType}")
      if ((nodata.isNaN() && !other.nodata.isNaN()) || (!nodata.isNaN() && nodata != other.nodata))
        sys.error(s"Error: All input tifs must have same nodata value ${nodata} != ${other.nodata}")

      Metadata(extent.combine(other.extent), pixelSize, pixels, bands, rasterType, nodata)
    }
  }

  /*
   * Get metadata out of the underlying tiff if it is accepted, 
   * and close the reader. If tiff is not accepted, return None
   */
  def getMetadata(path: Path, conf: Configuration): Option[Metadata] = _accepts(path, conf) match {
    case Some(_) => withReader(path, conf) { reader => Some(getMetadata(reader)) }
    case None    => None
  }

  /*
   * Get metadata out of an existing reader
   */
  def getMetadata(reader: AbstractGridCoverage2DReader): Metadata = {
    val coverage = getGridCoverage2D(reader)
    val envelope = coverage.getGridGeometry().gridToWorld(new GridEnvelope2D(0, 0, 1, 1));
    val pixelSize = (math.abs(envelope.getWidth), math.abs(envelope.getHeight))
    val pixels = (coverage.getRenderedImage().getWidth(), coverage.getRenderedImage().getHeight())
    val bands = coverage.getNumSampleDimensions
    val rasterType = coverage.getRenderedImage().getSampleModel().getDataType()
    val nodata = reader.asInstanceOf[GTGeoTiffReader].getMetadata().getNoData()
    val env = coverage.getEnvelope.asInstanceOf[GeneralEnvelope]
    val extent = Extent(env.getLowerCorner().getOrdinate(0), env.getLowerCorner().getOrdinate(1),
      env.getUpperCorner().getOrdinate(0), env.getUpperCorner().getOrdinate(1))
    Metadata(extent, pixelSize, pixels, bands, rasterType, nodata)
  }

  /*
   * Handles closing of readers using loan-pattern
   */
  def withReader[T](path: Path, conf: Configuration)(f: AbstractGridCoverage2DReader => T): T = {
    val reader = getReader(path, conf)
    val ret = f(reader)
    close(reader)
    ret
  }

  /*
   * Get GridCoverage2D out of existing reader. Does not close reader (see withReader) 
   */
  def getGridCoverage2D(reader: AbstractGridCoverage2DReader): GridCoverage2D =
    reader.read(null)

  def accepts(path: Path, conf: Configuration): Boolean = 
    _accepts(path, conf) match {
      case Some(_) => true
      case None    => false
    }

  private def _accepts(path: Path, conf: Configuration): Option[AbstractGridFormat] = {
    val stream = path.getFileSystem(conf).open(path)
    val afgOpt = _accepts(stream)
    stream.close()
    afgOpt
  }

  private def _accepts(obj: Object): Option[AbstractGridFormat] =
    formats.find(_.accepts(obj, hints))

  private def close(reader: AbstractGridCoverage2DReader): Unit =
    reader.getSource().asInstanceOf[FSDataInputStream].close

  private def getReader(path: Path, conf: Configuration): AbstractGridCoverage2DReader = {
    val stream = path.getFileSystem(conf).open(path)
    val format = 
      _accepts(stream) match {
        case Some(f) => f
        case None    => sys.error("Couldn't find format")
      }
    format.getReader(stream, hints)
  }

  private def loadFormats: Set[AbstractGridFormat] = {
    val spis = GridFormatFinder.getAvailableFormats()
    spis.map(_.createFormat())
  }

  private def printClassLoader: Unit = {
    val cl = ClassLoader.getSystemClassLoader()
    import java.net.URLClassLoader
    val urls = cl.asInstanceOf[URLClassLoader].getURLs()
    logInfo(urls.mkString(",\n"))
    logInfo("end printing classpath -------")
  }
}
