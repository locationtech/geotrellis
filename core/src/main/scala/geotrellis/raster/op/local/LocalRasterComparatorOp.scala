package geotrellis.raster.op.local

import geotrellis._
import geotrellis.raster._
import scalaxy.loops._

trait LocalTileComparatorOp extends Serializable {
  val name = {
    val n = getClass.getSimpleName
    if(n.endsWith("$")) n.substring(0, n.length-1)
    else n
  }
  // Tile - Constant combinations

  /** Apply to the value from each cell and a constant Int. */
  def apply(r: Op[Tile], c: Op[Int]): Op[Tile] = {
    (r, c).map(apply)
      .withName(s"$name[ConstantInt]")
  }

  /** Apply to the value from each cell and a constant Double. */
  def apply(r: Op[Tile], c: Op[Double])(implicit d: DI): Op[Tile] =
    (r, c).map(apply)
      .withName(s"$name[ConstantDouble]")

  /** Apply to a constant Int and the value from each cell. */
  def apply(c: Op[Int], r: Op[Tile])(implicit d: DI, d2: DI, d3: DI): Op[Tile] =
    (c, r).map(apply)
      .withName(s"$name[ConstantInt]")

  /** Apply to a constant Double and the value from each cell. */
  def apply(c: Op[Double], r: Op[Tile])(implicit d: DI, d2: DI, d3: DI, d4: DI): Op[Tile] =
    (c, r).map(apply)
      .withName(s"$name[ConstantDouble]")

  /** Apply to the value from each cell and a constant Int. */
  def apply(r: Tile, c: Int): Tile = {
    val tile = BitArrayTile.ofDim(r.cols, r.rows)
    for(col <- 0 until r.cols optimized) {
      for(row <- 0 until r.rows optimized) {
        tile.set(col, row, if(compare(r.get(col, row), c)) 1 else 0)
      }
    }
    tile
  }

  /** Apply to the value from each cell and a constant Double. */
  def apply(r: Tile, c: Double): Tile = {
    val tile = BitArrayTile.ofDim(r.cols, r.rows)

    for(col <- 0 until r.cols optimized) {
      for(row <- 0 until r.rows optimized) {
        tile.set(col, row, if(compare(r.getDouble(col, row), c)) 1 else 0)
      }
    }
    tile
  }

  /** Apply to a constant Int and the value from each cell. */
  def apply(c: Int, r: Tile): Tile = {
    val tile = BitArrayTile.ofDim(r.cols, r.rows)

    for(col <- 0 until r.cols optimized) {
      for(row <- 0 until r.rows optimized) {
        tile.set(col, row, if(compare(c, r.get(col, row))) 1 else 0)
      }
    }
    tile
  }

  /** Apply to a constant Double and the value from each cell. */
  def apply(c: Double, r: Tile): Tile = {
    val tile = BitArrayTile.ofDim(r.cols, r.rows)

    for(col <- 0 until r.cols optimized) {
      for(row <- 0 until r.rows optimized) {
        tile.set(col, row, if(compare(c, r.getDouble(col, row))) 1 else 0)
      }
    }
    tile
  }

  // Tile - Tile combinations

  /** Apply this operation to the values of each cell in each raster.  */
  def apply(r1: Op[Tile], r2: Op[Tile])(implicit d: DI, d2: DI): Op[Tile] =
    (r1, r2).map(apply)
      .withName(s"$name[Tiles]")

  /** Apply this operation to the values of each cell in each raster.  */
  def apply(r1: Tile, r2: Tile): Tile = {
    Seq(r1, r2).assertEqualDimensions
    val (cols, rows) = r1.dimensions
    val tile = BitArrayTile.ofDim(cols, rows)

    for(col <- 0 until cols optimized) {
      for(row <- 0 until rows optimized) {
        if(r1.cellType.isFloatingPoint) {
          tile.set(col, row, if(compare(r1.getDouble(col, row), r2.getDouble(col, row))) 1 else 0)
        }else {
          tile.set(col, row, if(compare(r1.get(col, row), r2.get(col, row))) 1 else 0)
        }
      }
    }
    tile
  }


  def compare(z1: Int, z2: Int): Boolean
  def compare(z1: Double, z2: Double): Boolean
}
