package geotrellis.data

import geotrellis._

import org.geotools.coverage.grid.GridCoverage2D
import org.geotools.coverage.grid.GridEnvelope2D
import org.geotools.factory.Hints
import org.geotools.gce
import org.geotools.gce.geotiff.{ GeoTiffReader => GTGeoTiffReader }
import org.geotools.geometry.GeneralEnvelope
import org.geotools.referencing.CRS

import java.awt.image.DataBuffer._
import java.io.File
import java.net.URL
/**
 * Utility class for dealing with GeoTiff files.
 */
object GeoTiff {
  case class Metadata(
    bounds: GeneralEnvelope,
    pixelDims: Tuple2[Double, Double],
    bands: Int,
    rasterType: Int,
    nodata: Double)

  def getMetadata(url: URL, epsg: String = "EPSG:3785"): Option[Metadata] =
    accepts(url, epsg) match {
      case false => None
      case true => {
        val coverage = getGridCoverage2D(url, epsg)
        val reader = getReader(url, epsg)		
        val envelope = coverage.getGridGeometry().gridToWorld(new GridEnvelope2D(0, 0, 1, 1));
        val pixelDims = (math.abs(envelope.getWidth), math.abs(envelope.getHeight))
        val bands = coverage.getNumSampleDimensions
        val rasterType = coverage.getRenderedImage().getSampleModel().getDataType()
        val nodata = reader.getMetadata().getNoData()
        Some(Metadata(coverage.getEnvelope.asInstanceOf[GeneralEnvelope], pixelDims, bands, rasterType, nodata))
      }
    }

  /* By default we get a EPSG:3785 reader. Since Scala doesn't allow multiple overloaded methods with 
   * default arguments, we have two variants of getReader that take URL instead of one 
   */
  def getReader(url: URL): GTGeoTiffReader =
    getReader(url.asInstanceOf[Object], "EPSG:3785")

  def getReader(url: URL, epsg: String): GTGeoTiffReader =
    getReader(url.asInstanceOf[Object], epsg)

  def getReader(path: String, epsg: String = "EPSG:3785"): GTGeoTiffReader = {
    val fh = new File(path)
    if (!fh.canRead) sys.error("can't read %s".format(path))

    getReader(fh, epsg)
  }

  private def accepts(o: Object, epsg: String): Boolean = {
    val gtf = new gce.geotiff.GeoTiffFormat
    val hints = new Hints(Hints.DEFAULT_COORDINATE_REFERENCE_SYSTEM, CRS.decode(epsg))

    gtf.accepts(o, hints)
  }
  private def getReader(o: Object, epsg: String): GTGeoTiffReader = {
    val gtf = new gce.geotiff.GeoTiffFormat
    val hints = new Hints(Hints.DEFAULT_COORDINATE_REFERENCE_SYSTEM, CRS.decode(epsg))

    gtf.getReader(o, hints)
  }

  def loadRasterExtent(path: String): RasterExtent =
    loadRasterExtent(getGridCoverage2D(path))

  def loadRasterExtent(cov: GridCoverage2D): RasterExtent = {
    val env = cov.getEnvelope2D
    val e = Extent(env.getMinX, env.getMinY, env.getMaxX, env.getMaxY)

    val data = cov.getRenderedImage.getData
    val rows = data.getHeight
    val cols = data.getWidth

    val cellwidth = (e.xmax - e.xmin) / cols
    val cellheight = (e.ymax - e.ymin) / rows

    RasterExtent(e, cellwidth, cellheight, cols, rows)
  }

  /* By default we use projection EPSG:3785. Since Scala doesn't allow multiple overloaded methods with 
   * default arguments, we have two variants of getGridCoverage2D that take URL instead of one  
   */
  def getGridCoverage2D(url: URL): GridCoverage2D =
    getReader(url).read(null)

  def getGridCoverage2D(url: URL, epsg: String): GridCoverage2D =
    getReader(url, epsg).read(null)

  def getGridCoverage2D(path: String, epsg: String = "EPSG:3785"): GridCoverage2D =
    getReader(path, epsg).read(null)

  def getGridCoverage2D(reader: gce.geotiff.GeoTiffReader): GridCoverage2D =
    reader.read(null)

  def getDataType(reader: gce.geotiff.GeoTiffReader): RasterType =
    getDataType(getGridCoverage2D(reader))

  def getDataType(cov: GridCoverage2D): RasterType = {
    cov.getRenderedImage.getSampleModel.getDataType match {
      // Bytes are unsigned in GTiff
      case TYPE_BYTE   => TypeShort
      case TYPE_SHORT  => TypeShort
      case TYPE_USHORT => TypeInt
      case TYPE_INT    => TypeInt
      case TYPE_FLOAT  => TypeFloat
      case TYPE_DOUBLE => TypeDouble
      case _           => TypeDouble
    }
  }

  def readRaster(path: String) = {
    val reader = getReader(path)
    val cov = getGridCoverage2D(reader)
    val rasterExtent = loadRasterExtent(cov)
    val rasterType = getDataType(cov)

    val readState =
      if (rasterType.isDouble) {
        new GeoTiffDoubleReadState(path, rasterExtent, rasterExtent, rasterType, reader)
      } else {
        new GeoTiffIntReadState(path, rasterExtent, rasterExtent, rasterType, reader)
      }

    val raster = readState.loadRaster()
    readState.destroy()
    raster
  }
}
