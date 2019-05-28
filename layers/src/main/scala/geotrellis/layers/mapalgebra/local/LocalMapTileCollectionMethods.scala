package geotrellis.layers.mapalgebra.local

import geotrellis.raster._
import geotrellis.layers._
import geotrellis.util.MethodExtensions


trait LocalMapTileCollectionMethods[K] extends MethodExtensions[Seq[(K, Tile)]] {
    /** Map the integer values of a each cell to a new integer value. */
  def localMap(f: Int => Int) =
    self.mapValues { tile =>
       tile.dualMap(f)({ z: Double => i2d(f(d2i(z))) })
    }

  /** Map the double values of a each cell to a new double value. */
  def localMapDouble(f: Double => Double) =
    self.mapValues { tile =>
      tile.dualMap({ z: Int => d2i(f(i2d(z))) })(f)
    }

  /** For each cell whose value is not NODATA, map their integer values to a new integer value */
  def localMapIfSet(f: Int => Int) =
    self.mapValues { tile =>
      tile.dualMapIfSet(f)({ z: Double => i2d(f(d2i(z))) })
    }

  /** For each cell whose value is not Double.NaN, map their double values to a new integer value */
  def localMapIfSetDouble(f: Double => Double) =
    self.mapValues { tile =>
      tile.dualMapIfSet({ z: Int => d2i(f(i2d(z))) })(f)
    }

  /** Map the values of a each cell to a new value;
      *if the type of the raster is a double type, map using the
      *double function, otherwise map using the integer function. */
  def localDualMap(fInt: Int => Int)(fDouble: Double => Double) =
    self.mapValues { tile =>
      tile.dualMap(fInt)(fDouble)
    }

  /** For each cell whose value is not a NoData, if the type of the raster is a double type,
      *map using the double function, otherwise map using the integer function. */
  def localMapIfSetDouble(fInt: Int => Int)(fDouble: Double => Double) =
    self.mapValues { tile =>
      tile.dualMapIfSet(fInt)(fDouble)
    }
}
