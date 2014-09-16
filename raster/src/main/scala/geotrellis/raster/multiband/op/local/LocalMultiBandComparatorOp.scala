package geotrellis.raster.multiband.op.local

import geotrellis.raster._
import geotrellis.raster.multiband._

import spire.syntax.cfor._

trait LocalMultiBandComparatorOp extends Serializable {
  val name = {
    val n = getClass.getSimpleName
    if (n.endsWith("$")) n.substring(0, n.length - 1)
    else n
  }
  // MultiBandTile - Constant combinations

  /** Apply to the value from each cell of each band in a multiband raster and a constant Int. */
  def apply(m: MultiBandTile, c: Int): MultiBandTile = {
    val multiband = Array.ofDim[Tile](m.bands)
    cfor(0)(_ < m.bands, _ + 1) { band =>
      var tile = BitArrayTile.ofDim(m.cols, m.rows)
      cfor(0)(_ < m.cols, _ + 1) { col =>
        cfor(0)(_ < m.rows, _ + 1) { row =>
          tile.set(col, row, if (compare(m.getBand(band).get(col, row), c)) 1 else 0)
        }
      }
      multiband.update(band, tile)
    }
    MultiBandTile(multiband)
  }

  /** Apply to the value from each cell of each band in a multiband raster and a constant Double. */
  def apply(m: MultiBandTile, c: Double): MultiBandTile = {
    val multiband = Array.ofDim[Tile](m.bands)
    cfor(0)(_ < m.bands, _ + 1) { band =>
      var tile = BitArrayTile.ofDim(m.cols, m.rows)
      cfor(0)(_ < m.cols, _ + 1) { col =>
        cfor(0)(_ < m.rows, _ + 1) { row =>
          tile.set(col, row, if (compare(m.getBand(band).getDouble(col, row), c)) 1 else 0)
        }
      }
      multiband(band) = tile
    }
    MultiBandTile(multiband)
  }

  /** Apply to a constant Int and the value from each cell of each band in a multiband raster. */
  def apply(c: Int, m: MultiBandTile): MultiBandTile = {
    val multiband = Array.ofDim[Tile](m.bands)
    cfor(0)(_ < m.bands, _ + 1) { band =>
      var tile = BitArrayTile.ofDim(m.cols, m.rows)
      cfor(0)(_ < m.cols, _ + 1) { col =>
        cfor(0)(_ < m.rows, _ + 1) { row =>
          tile.set(col, row, if (compare(c, m.getBand(band).get(col, row))) 1 else 0)
        }
      }
      multiband(band) = tile
    }
    MultiBandTile(multiband)
  }

  /** Apply to a constant Double and the value from each cell of each band in a multiband raster. */
  def apply(c: Double, m: MultiBandTile): MultiBandTile = {
    val multiband = Array.ofDim[Tile](m.bands)
    cfor(0)(_ < m.bands, _ + 1) { band =>
      var tile = BitArrayTile.ofDim(m.cols, m.rows)
      cfor(0)(_ < m.cols, _ + 1) { col =>
        cfor(0)(_ < m.rows, _ + 1) { row =>
          tile.set(col, row, if (compare(c, m.getBand(band).getDouble(col, row))) 1 else 0)
        }
      }
      multiband(band) = tile
    }
    MultiBandTile(multiband)
  }

  // MultiBandTile - MultiBandTile combinations

  /** Apply this operation to the values of each cell in each bands of each multiband rasters.  */
  def apply(m1: MultiBandTile, m2: MultiBandTile): MultiBandTile = {
    assert(m1.bands == m2.bands, "Number of bands in MultiBandTile are no equal")
    assert(m1.dimensions == m2.dimensions, "Number of Cols and Rows in bands are not equal")
    val (cols, rows) = m1.dimensions
    val multiband = Array.ofDim[Tile](m1.bands)
    cfor(0)(_ < m1.bands, _ + 1) { band =>
      var tile = BitArrayTile.ofDim(cols, rows)
      cfor(0)(_ < cols, _ + 1) { col =>
        cfor(0)(_ < rows, _ + 1) { row =>
          if (m1.cellType.isFloatingPoint) {
            tile.set(col, row, if (compare(m1.getBand(band).getDouble(col, row), m2.getBand(band).getDouble(col, row))) 1 else 0)
          } else {
            tile.set(col, row, if (compare(m1.getBand(band).get(col, row), m2.getBand(band).get(col, row))) 1 else 0)
          }
        }
      }
      multiband(band) = tile
    }
    MultiBandTile(multiband)
  }

  def compare(z1: Int, z2: Int): Boolean
  def compare(z1: Double, z2: Double): Boolean
}
