package geotrellis.raster.mapalgebra.focal.tobler

import geotrellis.raster._
import geotrellis.raster.mapalgebra.local._
import geotrellis.raster.mapalgebra.focal._

object Tobler {

  def apply(r: Tile, n: Neighborhood, bounds: Option[GridBounds], cs: CellSize, z: Double, target: TargetCell = TargetCell.All): Tile = {

    val slope = Slope(r, n, bounds, cs ,z, target)

    6 *: (math.E **: (((slope * math.Pi / 180.0).localTan + 0.05).localAbs * (-3.5)))
  }

}
