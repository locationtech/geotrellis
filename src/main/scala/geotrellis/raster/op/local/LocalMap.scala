/*******************************************************************************
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
 ******************************************************************************/

package geotrellis.raster.op.local

import geotrellis._
import geotrellis.source._

/**
 * Perform a function on every cell in a raster.
 *
 * @example
 * <pre>
 * val r = LoadFile(f)
 * val result = LocalMap(R, {x:Int => x + 3} ) // add 3 to every cell in the raster  
 * </pre>
 */
object LocalMap extends Serializable {
  def apply(r:Op[Raster])(f:Int => Int) =
    r.map(_.dualMap(f)({ z:Double => i2d(f(d2i(z))) }))
     .withName("LocalMap")

  /**
   * Perform a function on every cell in a raster with the values from another raster.
   *
   * @example
   * <pre>
   * val r1 = LoadFile(a)
   * val r2 = LoadFile(b)
   * 
   * // Generate a raster by adding the values of each cell in A and B 
   * val result = BinaryLocalMap(r1, r2, {(a:Int, b:Int) => a + b} )
   * </pre>
   */
  def apply(r1:Op[Raster], r2:Op[Raster])(f:(Int,Int) => Int) = 
    (r1,r2).map { (a,b) => a.dualCombine(b)(f)((z1:Double, z2:Double) => i2d(f(d2i(z1), d2i(z2)))) }
           .withName("LocalMap")
}
    
/**
 * Perform a Double function on every cell in a raster.
 *
 * @example
 * <pre>
 * val r = LoadFile(f)
 * val r2 = LocalMap(R, x => x + 3 ) // add 3 to every cell in the raster  
 * </pre>
 */
object  LocalMapDouble extends Serializable {
  def apply(r:Op[Raster])(f:Double => Double) =
    r.map(_.dualMap({ z:Int => d2i(f(i2d(z))) })(f))
     .withName("LocalMapDouble")

  /**
   * Perform a Double function on every cell in a raster with the values from another raster.
   *
   * @example
   * <pre>
   * val r1 = LoadFile(a)
   * val r2 = LoadFile(b)
   * 
   * // Generate a raster by adding the values of each cell in A and B 
   * val result = BinaryLocalMap(r1, r2, {(a:Double, b:Double) => a + b} )
   * </pre>
   */
  def apply(r1:Op[Raster], r2:Op[Raster])(f:(Double,Double) => Double) = 
    (r1,r2).map { (a,b) => a.dualCombine(b)((z1:Int,z2:Int)=>d2i(f(i2d(z1), i2d(z2))))(f) }
           .withName("LocalMap")
}

trait LocalMapOpMethods[+Repr <: RasterSource] { self: Repr =>
    /** Map the integer values of a each cell to a new integer value. */
  def localMap(f:Int=>Int) = 
    self.mapOp { tileOp =>
      tileOp.map(_.dualMap(f)({ z:Double => i2d(f(d2i(z))) }))
            .withName("LocalMap")
    }

  /** Map the double values of a each cell to a new double value. */
  def localMapDouble(f:Double=>Double) = 
    self.mapOp { tileOp =>
      tileOp.map(_.dualMap({ z:Int => d2i(f(i2d(z))) })(f))
            .withName("LocalMapDouble")
    }

  /** For each cell whose value is not NODATA, map their integer values to a new integer value */
  def localMapIfSet(f:Int=>Int) = 
    self.mapOp { tileOp =>
      tileOp.map(_.dualMapIfSet(f)({ z:Double => i2d(f(d2i(z))) }))
            .withName("LocalMapIfSet")
    }

  /** For each cell whose value is not Double.NaN, map their double values to a new integer value */
  def localMapIfSetDouble(f:Double=>Double) = 
    self.mapOp { tileOp =>
      tileOp.map(_.dualMapIfSet({ z:Int => d2i(f(i2d(z))) })(f))
            .withName("LocalMapIfSetDouble")
    }

  /** Map the values of a each cell to a new value;
      if the type of the raster is a double type, map using the 
      double function, otherwise map using the integer function. */
  def localDualMap(fInt:Int=>Int)(fDouble:Double=>Double) =
    self.mapOp { tileOp =>
      tileOp.map(_.dualMap(fInt)(fDouble))
            .withName("LocalDualMap")
    }

  /** For each cell whose value is not a NoData, if the type of the raster is a double type, 
      map using the double function, otherwise map using the integer function. */
  def localMapIfSetDouble(fInt:Int=>Int)(fDouble:Double=>Double) = 
    self.mapOp { tileOp =>
      tileOp.map(_.dualMapIfSet(fInt)(fDouble))
            .withName("LocalDualMapIfSet")
    }
}
