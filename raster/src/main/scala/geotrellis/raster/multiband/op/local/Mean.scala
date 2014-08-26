package geotrellis.raster.multiband.op.local

import geotrellis.raster._
import geotrellis.raster.multiband._

/**
 * The mean of values at each location in a set of Tiles.
 */
object Mean extends Serializable {
  def apply(m: MultiBandTile): Tile = {
    m.dualCombine(0, m.bands - 1)((i, j) => i + j)((i, j) => i + j).dualMap(k => k / (m.bands))(k => k / (m.bands))
  }

  def apply(m: MultiBandTile, f: Int, l: Int): Tile = {
    m.dualCombine(f, l)((i, j) => i + j)((i, j) => i + j).dualMap(k => k / (l - f + 1))(k => k / (l - f + 1))
  }
  
}