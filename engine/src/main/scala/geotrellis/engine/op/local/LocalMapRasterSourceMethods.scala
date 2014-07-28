/*
 * Copyright (c) 2014 Azavea.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package geotrellis.engine.op.local

import geotrellis.engine._
import geotrellis.raster._
import geotrellis.raster.op.local._

trait LocalMapRasterSourceMethods extends RasterSourceMethods {
    /** Map the integer values of a each cell to a new integer value. */
  def localMap(f:Int=>Int) = 
    rasterSource.mapTile { tile =>
       tile.dualMap(f)({ z:Double => i2d(f(d2i(z))) })
    }

  /** Map the double values of a each cell to a new double value. */
  def localMapDouble(f:Double=>Double) = 
    rasterSource.mapTile { tile =>
      tile.dualMap({ z:Int => d2i(f(i2d(z))) })(f)
    }

  /** For each cell whose value is not NODATA, map their integer values to a new integer value */
  def localMapIfSet(f:Int=>Int) = 
    rasterSource.mapTile { tile =>
      tile.dualMapIfSet(f)({ z:Double => i2d(f(d2i(z))) })
    }

  /** For each cell whose value is not Double.NaN, map their double values to a new integer value */
  def localMapIfSetDouble(f:Double=>Double) = 
    rasterSource.mapTile { tile =>
      tile.dualMapIfSet({ z:Int => d2i(f(i2d(z))) })(f)
    }

  /** Map the values of a each cell to a new value;
      if the type of the raster is a double type, map using the 
      double function, otherwise map using the integer function. */
  def localDualMap(fInt:Int=>Int)(fDouble:Double=>Double) =
    rasterSource.mapTile { tile =>
      tile.dualMap(fInt)(fDouble)
    }

  /** For each cell whose value is not a NoData, if the type of the raster is a double type, 
      map using the double function, otherwise map using the integer function. */
  def localMapIfSetDouble(fInt:Int=>Int)(fDouble:Double=>Double) = 
    rasterSource.mapTile { tile =>
      tile.dualMapIfSet(fInt)(fDouble)
    }
}
