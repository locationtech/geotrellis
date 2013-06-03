package geotrellis.data

import geotrellis._

import java.io.File
import java.awt.image.DataBuffer._

import org.geotools.factory.Hints
import org.geotools.gce
import org.geotools.referencing.CRS
import org.geotools.coverage.grid.GridCoverage2D

/**
 * Utility class for dealing with GeoTiff files.
 */
object GeoTiff {
  def getReader(path:String) = {
    val fh    = new File(path)
    val gtf   = new gce.geotiff.GeoTiffFormat
    val hints = new Hints(Hints.DEFAULT_COORDINATE_REFERENCE_SYSTEM, CRS.decode("EPSG:3785"))

    if (!fh.canRead) sys.error("can't read %s".format(path))

    gtf.getReader(fh, hints)
  }

  def loadRasterExtent(path:String):RasterExtent = 
    loadRasterExtent(getGridCoverage2D(path))

  def loadRasterExtent(cov:GridCoverage2D):RasterExtent = {
    val env  = cov.getEnvelope2D
    val e = Extent(env.getMinX, env.getMinY, env.getMaxX, env.getMaxY)

    val data = cov.getRenderedImage.getData
    val rows = data.getHeight
    val cols = data.getWidth

    val cellwidth  = (e.xmax - e.xmin) / cols
    val cellheight = (e.ymax - e.ymin) / rows

    RasterExtent(e, cellwidth, cellheight, cols, rows)
  }

  def getGridCoverage2D(path:String):GridCoverage2D = 
    getReader(path).read(null)

  def getGridCoverage2D(reader:gce.geotiff.GeoTiffReader):GridCoverage2D = 
    reader.read(null)

  def getDataType(reader:gce.geotiff.GeoTiffReader):RasterType = 
    getDataType(getGridCoverage2D(reader))

  def getDataType(cov:GridCoverage2D):RasterType = {
    cov.getRenderedImage.getSampleModel.getDataType match {
      // Bytes are unsigned in GTiff
      case TYPE_BYTE    => TypeShort
      case TYPE_SHORT   => TypeShort
      case TYPE_USHORT  => TypeInt
      case TYPE_INT     => TypeInt
      case TYPE_FLOAT   => TypeFloat
      case TYPE_DOUBLE  => TypeDouble
      case _            => TypeDouble
    }
  }

  def readRaster(path:String) = {
    val reader = getReader(path)
    val cov = getGridCoverage2D(reader)
    val rasterExtent = loadRasterExtent(cov)
    val rasterType = getDataType(cov)

    val readState = 
      if(rasterType.isDouble) {
        new GeoTiffDoubleReadState(path, rasterExtent, rasterExtent, rasterType, reader)
      } else {
        new GeoTiffIntReadState(path, rasterExtent, rasterExtent, rasterType, reader)
      }

    val raster = readState.loadRaster()
    readState.destroy()
    raster
  }
}
