/**
 *
 */
package geotrellis.raster.multiband.op.local

import geotrellis.raster._
import geotrellis.raster.multiband._

trait LocalMultiBandBinaryOp extends Serializable {

  val name = {
    val n = getClass.getSimpleName
    if (n.endsWith("$")) n.substring(0, n.length - 1)
    else n
  }

  // MultiBandTile - Constant combinations

  /** Apply to the value from each cell of each band in a MultiBandTile and a constant Int. */
  def apply(m: MultiBandTile, c: Int): MultiBandTile =
    m.dualMap(combine(_, c))({
      val d = i2d(c)
      (z: Double) => combine(z, d)
    })

  /** Apply to the value from each cell of each band in a MultiBandTile and a constant Double. */
  def apply(m: MultiBandTile, c: Double): MultiBandTile =
    m.dualMap({ z => d2i(combine(i2d(z), c)) })(combine(_, c))

  /** Apply to a constant Int and the value from each cell of each band in a MultiBandTile. */
  def apply(c: Int, m: MultiBandTile): MultiBandTile =
    m.dualMap(combine(c, _))({
      val d = i2d(c)
      (z: Double) => combine(d, z)
    })

  /** Apply to a constant Double and the value from each cell of each band in a MultiBandTile. */
  def apply(c: Double, m: MultiBandTile): MultiBandTile =
    m.dualMap({ z => d2i(combine(c, i2d(z))) })(combine(c, _))

  // MultiBandTile - combination of inner bands

  /** Apply this operation to the values of each cell in each band in a MultiBandTile. */
  def apply(m: MultiBandTile): Tile =
    m.dualCombine(0, m.bands - 1)(combine)(combine)

  /** Apply this operation to the values of each cell in given range of bands in a MultiBandTile. */
  def apply(m: MultiBandTile, first: Int, last: Int): Tile =
    m.dualCombine(first, last)(combine)(combine)

  /** Apply this operation to the values of each cell in given range of bands in a MultiBandTile. */
  def apply(first: Int, last: Int, m: MultiBandTile): Tile =
    m.dualCombine(first, last)(combine)(combine)

  // MultiBandTile - MultiBandTile combinations

  /** Apply this operation to the values of each cell in each band in each MultiBandTiles.  */
  def apply(m1: MultiBandTile, m2: MultiBandTile): MultiBandTile =
    m1.dualCombine(m2)(combine)(combine)

  def combine(z1: Int, z2: Int): Int
  def combine(z1: Double, z2: Double): Double
}