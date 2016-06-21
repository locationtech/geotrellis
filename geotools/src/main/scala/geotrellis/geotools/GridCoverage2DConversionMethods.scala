package geotrellis.geotools

import geotrellis.proj4.LatLng
import geotrellis.raster._
import geotrellis.util._

import org.geotools.coverage.grid._
import org.geotools.resources.coverage.CoverageUtilities
import spire.syntax.cfor._

import java.awt.image.{Raster => AwtRaster, _}

trait GridCoverage2DConversionMethods extends MethodExtensions[GridCoverage2D] {
  def toTile(bandIndex: Int): Tile =
    GridCoverage2DConverters.convertToTile(self, bandIndex)

  def toRaster(bandIndex: Int): Raster[Tile] = {
    val tile = GridCoverage2DConverters.convertToTile(self, bandIndex)
    val extent = GridCoverage2DConverters.getExtent(self)

    Raster(tile, extent)
  }

  def toRaster(): Raster[MultibandTile] = {
    val renderedImage = self.getRenderedImage
    val sampleModel = renderedImage.getSampleModel
    val numBands = sampleModel.getNumBands

    val tile = {
      val tiles = Array.ofDim[Tile](numBands)
      cfor(0)(_ < numBands, _ + 1) { b =>
        tiles(b) = GridCoverage2DConverters.convertToTile(self, b)
      }
      ArrayMultibandTile(tiles)
    }
    val extent = GridCoverage2DConverters.getExtent(self)

    Raster(tile, extent)
  }

  def toProjectedRaster(bandIndex: Int): ProjectedRaster[Tile] =
    GridCoverage2DConverters.getCrs(self) match {
      case Some(crs) =>
        ProjectedRaster(toRaster(bandIndex), crs)
      case None =>
        // Default LatLng
        ProjectedRaster(toRaster(bandIndex), LatLng)
    }

  def toProjectedRaster(): ProjectedRaster[MultibandTile] =
    GridCoverage2DConverters.getCrs(self) match {
      case Some(crs) =>
        ProjectedRaster(toRaster(), crs)
      case None =>
        // Default LatLng
        ProjectedRaster(toRaster(), LatLng)
    }

}
