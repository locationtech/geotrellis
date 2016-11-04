package geotrellis.doc.examples.raster

import geotrellis.proj4.CRS
import geotrellis.raster._
import geotrellis.vector.Extent
import geotrellis.raster.io.geotiff._
import geotrellis.raster.reproject.Reproject
import geotrellis.raster.resample.{NearestNeighbor, ResampleMethod}

object MatchingRasters {
  def `Matching two rasters of a different CRS so that you can perform operations between them. [1]` = {
    val raster1: ProjectedRaster[Tile] = ???
    val raster2: ProjectedRaster[Tile] = ???
    val areaOfInterest: Extent = ???

    // Weights for our weighted sum
    val (w1, w2): (Int, Int) = ???

    val cropped1 =
      raster1.raster.crop(areaOfInterest)

    val cropped2 =
      raster2
        .reproject(raster1.crs).raster
        .resample(raster1.rasterExtent)
        .crop(areaOfInterest)

    val result = cropped1.tile * w1 + cropped2.tile * w2
  }

  def `Matching two rasters of a different CRS so that you can perform operations between them. [2]` = {
    val raster1: ProjectedRaster[Tile] = ???
    val raster2: ProjectedRaster[Tile] = ???
    val areaOfInterest: Extent = ???

    // Weights for our weighted sum
    val (w1, w2): (Int, Int) = ???

    val options = Reproject.Options(
      targetRasterExtent = Some(raster1.rasterExtent)
    )

    val cropped1 =
      raster1.raster.crop(areaOfInterest)

    val cropped2 =
      raster2
        .reproject(raster1.crs, options)

    val result = cropped1.tile * w1 + cropped2.tile * w2
  }
}
