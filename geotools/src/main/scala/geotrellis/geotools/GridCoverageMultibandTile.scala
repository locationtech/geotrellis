package geotrellis.geotools

import geotrellis.raster._

import org.geotools.coverage.grid._
import org.geotools.gce.geotiff._
import org.geotools.coverage.grid.io._


class GridCoverageMultibandTile(gridCoverage: GridCoverage2D) extends MultibandTile {
  val renderedImage = gridCoverage.getRenderedImage
  val buffer = renderedImage.getData.getDataBuffer

  def bandCount = renderedImage.getSampleModel.getNumBands

  def cellType: CellType = {
    // XXX handle NODATA
   buffer.getDataType match {
      case 0 => ByteCellType
      case _ => throw new Exception("Unknown CellType")
    }
  }

  def rows: Int = renderedImage.getHeight

  def cols: Int = renderedImage.getWidth
}
