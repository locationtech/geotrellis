package geotrellis.raster

import geotrellis.raster.reproject._
import geotrellis.vector.Extent
import geotrellis.proj4.CRS

object ProjectedRaster {
  implicit def tup3ToRaster(tup: (Tile, Extent, CRS)): ProjectedRaster =
    ProjectedRaster(Raster(tup._1, tup._2), tup._3)

  implicit def tup3SwapToRaster(tup: (Extent, Tile, CRS)): ProjectedRaster =
    ProjectedRaster(Raster(tup._2, tup._1), tup._3)

  implicit def tup3Swap2ToRaster(tup: (Tile, CRS, Extent)): ProjectedRaster =
    ProjectedRaster(Raster(tup._1, tup._3), tup._2)

  implicit def tup3Swap3ToRaster(tup: (Extent, CRS, Tile)): ProjectedRaster =
    ProjectedRaster(Raster(tup._3, tup._1), tup._2)

  implicit def tup3Swap4ToRaster(tup: (CRS, Tile, Extent)): ProjectedRaster =
    ProjectedRaster(Raster(tup._2, tup._3), tup._1)

  implicit def tup3Swap5ToRaster(tup: (CRS, Extent, Tile)): ProjectedRaster =
    ProjectedRaster(Raster(tup._3, tup._2), tup._1)

  implicit def tupToRaster(tup: (Raster, CRS)): ProjectedRaster =
    ProjectedRaster(tup._1, tup._2)

  implicit def tupSwapToRaster(tup: (CRS, Raster)): ProjectedRaster =
    ProjectedRaster(tup._2, tup._1)
}

case class ProjectedRaster(raster: Raster, crs: CRS) {
  def reproject(dest: CRS): Raster = 
    raster.reproject(crs, dest)
}
