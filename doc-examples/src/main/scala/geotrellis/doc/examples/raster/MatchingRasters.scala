package geotrellis.doc.examples.raster

import geotrellis.raster._

object MatchingRasters {
  def `Matching two rasters of a different CRS so that you can perform operations between them. [1]` = {
    val raster1: ProjectedRaster[Tile] = ???
    val raster2: ProjectedRaster[Tile] = ???
    val areaOfInterest: Extent = ???

    // Weights for our weighted sum
    val (w1, w2): (Int, Int) = ???

    val cropped1 =
      raster1.crop(areaOfInterest)

    val cropped2 =
      raster2
        .reproject(raster1.crs)
        .resample(raster1.rasterExtent)
        .crop(areaOfInterest)

    val result = (cropped1 * w1) + (cropped2 * w2)
  }

  def `Matching two rasters of a different CRS so that you can perform operations between them. [2]` = {
    val raster1: ProjectedRaster[Tile] = ???
    val raster2: ProjectedRaster[Tile] = ???
    val areaOfInterest: Extent = ???

    // Weights for our weighted sum
    val (w1, w2): (Int, Int) = ???

    val cropped1 =
      raster1.crop(areaOfInterest)

    val cropped2 =
      raster2
        .reproject(raster1.crs)
  }
}
