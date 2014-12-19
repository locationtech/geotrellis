package geotrellis.raster.op.local

import geotrellis.raster._

import spire.syntax.cfor._

/**
  * Implements the incremental algorithm for computing the variance in one sweep.
  *
  * The integer tiles use double precision during the computation since otherwise
  * the algorithm loses its numerical stability.
  */
object Variance extends Serializable {

  def apply(rs: Traversable[Tile]): Tile =
    apply(rs.toSeq)

  def apply(rs: Tile*)(implicit d: DI): Tile =
    apply(rs)

  def apply(rs: Seq[Tile]): Tile = {
    rs.assertEqualDimensions

    val layerCount = rs.length
    if (layerCount == 0) sys.error(s"Can't compute variance of empty sequence.")
    else {
      val newCellType = rs.map(_.cellType).reduce(_.union(_))
      val (cols, rows) = rs(0).dimensions
      val tile = ArrayTile.alloc(newCellType, cols, rows)

      cfor(0)(_ < cols, _ + 1) { row =>
        cfor(0)(_ < rows, _ + 1) { col =>
          var count = 0
          var mean = 0.0
          var m2 = 0.0

          cfor(0)(_ < layerCount, _ + 1) { i =>
            val v = rs(i).getDouble(col, row)
            if (isData(v)) {
              count += 1
              val delta = v - mean
              mean += delta / count
              m2 += delta * (v - mean)
            }
          }

          if (newCellType.isFloatingPoint) {
            if (count > 0) tile.setDouble(col, row, m2 / (count - 1))
            else tile.setDouble(col, row, Double.NaN)
          } else {
            if (count > 0) tile.set(col, row, (m2 / (count - 1)).round.toInt)
            else tile.set(col, row, NODATA)
          }
        }
      }

      tile
    }
  }
}
