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

package geotrellis

import geotrellis._
import geotrellis.raster.op.local._
import language.implicitConversions

object Implicits {
  import geotrellis._

  /**
   * Addition-operator implicits for Int, Raster and Op[Raster].
   */
  implicit def addOpRasterTo1(lhs:Op[Raster]) = new {
    def +(rhs:Int):Op[Raster] = Add(lhs, Literal(rhs))
    def +(rhs:Op[Int]):Op[Raster] = Add(lhs, rhs)
  }
  implicit def addOpRasterTo2(lhs:Op[Raster]) = new {
    def +(rhs:Raster):Op[Raster] = Add(lhs, Literal(rhs))
    def +(rhs:Op[Raster]):Op[Raster] = Add(lhs, rhs)
  }

  implicit def addRasterTo1(lhs:Raster) = new {
    def +(rhs:Int):Op[Raster] = Add(Literal(lhs), Literal(rhs))
    def +(rhs:Op[Int]):Op[Raster] = Add(Literal(lhs), rhs)
  }
  implicit def addRasterTo2(lhs:Raster) = new {
    def +(rhs:Raster):Op[Raster] = Add(Literal(lhs), Literal(rhs))
    def +(rhs:Op[Raster]):Op[Raster] = Add(Literal(lhs), rhs)
  }

  implicit def addOpIntTo1(lhs:Op[Int]) = new {
    def +(rhs:Int):Op[Int] = lhs.map(_ + rhs)
    def +(rhs:Op[Int]):Op[Int] = OpMap2(lhs,rhs).map(_+_)
  }
  implicit def addOpIntTo2(lhs:Op[Int]) = new {
    def +(rhs:Raster):Op[Raster] = Add(Literal(rhs), lhs)
    def +(rhs:Op[Raster]):Op[Raster] = Add(rhs, lhs)
  }

  implicit def addIntTo1(lhs:Int) = new {
    def +(rhs:Op[Int]):Op[Int] = rhs.map(_+lhs)
  }
  implicit def addIntTo2(lhs:Int) = new {
    def +(rhs:Raster):Op[Raster] = Add(Literal(rhs), Literal(lhs))
    def +(rhs:Op[Raster]):Op[Raster] = Add(rhs, Literal(lhs))
  }

  /**
   * Multiplication-operator implicits for Int, Raster and Op[Raster].
   */
  implicit def multiplyOpRasterBy1(lhs:Op[Raster]) = new {
    def *(rhs:Int):Op[Raster] = Multiply(lhs, Literal(rhs))
    def *(rhs:Op[Int]):Op[Raster] = Multiply(lhs, rhs)
  }
  implicit def multiplyOpRasterBy2(lhs:Op[Raster]) = new {
    def *(rhs:Raster):Op[Raster] = Multiply(lhs, Literal(rhs))
    def *(rhs:Op[Raster]):Op[Raster] = Multiply(lhs, rhs)
  }

  implicit def multiplyRasterBy1(lhs:Raster) = new {
    def *(rhs:Int):Op[Raster] = Multiply(Literal(lhs), Literal(rhs))
    def *(rhs:Op[Int]):Op[Raster] = Multiply(Literal(lhs), rhs)
  }
  implicit def multiplyRasterBy2(lhs:Raster) = new {
    def *(rhs:Raster):Op[Raster] = Multiply(Literal(lhs), Literal(rhs))
    def *(rhs:Op[Raster]):Op[Raster] = Multiply(Literal(lhs), rhs)
  }

  implicit def multiplyOpIntBy1(lhs:Op[Int]) = new {
    def *(rhs:Int):Op[Int] = lhs.map(_*rhs)
    def *(rhs:Op[Int]):Op[Int] = OpMap2(lhs,rhs).map(_*_)
  }
  implicit def multiplyOpIntBy2(lhs:Op[Int]) = new {
    def *(rhs:Raster):Op[Raster] = Multiply(Literal(rhs), lhs)
    def *(rhs:Op[Raster]):Op[Raster] = Multiply(rhs, lhs)
  }

  implicit def multiplyIntBy1(lhs:Int) = new {
    def *(rhs:Op[Int]):Op[Int] = rhs.map(_*lhs)
  }
  implicit def multiplyIntBy2(lhs:Int) = new {
    def *(rhs:Raster):Op[Raster] = Multiply(Literal(rhs), Literal(lhs))
    def *(rhs:Op[Raster]):Op[Raster] = Multiply(rhs, Literal(lhs))
  }

  /**
   * Subtraction-operator implicits for Int, Raster and Op[Raster].
   */
  implicit def subtractOpRasterBy1(lhs:Op[Raster]) = new {
    def -(rhs:Int):Op[Raster] = Subtract(lhs, Literal(rhs))
    def -(rhs:Op[Int]):Op[Raster] = Subtract(lhs, rhs)
  }
  implicit def subtractOpRasterBy2(lhs:Op[Raster]) = new {
    def -(rhs:Raster):Op[Raster] = Subtract(lhs, Literal(rhs))
    def -(rhs:Op[Raster]):Op[Raster] = Subtract(lhs, rhs)
  }

  implicit def subtractRasterBy1(lhs:Raster) = new {
    def -(rhs:Int):Op[Raster] = Subtract(Literal(lhs), Literal(rhs))
    def -(rhs:Op[Int]):Op[Raster] = Subtract(Literal(lhs), rhs)
  }
  implicit def subtractRasterBy2(lhs:Raster) = new {
    def -(rhs:Raster):Op[Raster] = Subtract(Literal(lhs), Literal(rhs))
    def -(rhs:Op[Raster]):Op[Raster] = Subtract(Literal(lhs), rhs)
  }

  implicit def subtractOpIntBy1(lhs:Op[Int]) = new {
    def -(rhs:Int):Op[Int] = lhs.map(_-rhs)
    def -(rhs:Op[Int]):Op[Int] = OpMap2(lhs,rhs).map(_-_)
  }
  implicit def subtractOpIntBy2(lhs:Op[Int]) = new {
    def -(rhs:Raster):Op[Raster] = Subtract(lhs, Literal(rhs))
    def -(rhs:Op[Raster]):Op[Raster] = Subtract(lhs, rhs)
  }

  implicit def subtractIntBy1(lhs:Int) = new {
    def -(rhs:Op[Int]):Op[Int] = rhs.map(lhs-_)
  }
  implicit def subtractIntBy2(lhs:Int) = new {
    def -(rhs:Raster):Op[Raster] = Subtract(Literal(lhs), Literal(rhs))
    def -(rhs:Op[Raster]):Op[Raster] = Subtract(Literal(lhs), rhs)
  }

  /**
   * Division-operator implicits for Int, Raster and Op[Raster].
   */
  implicit def divideOpRasterBy1(lhs:Op[Raster]) = new {
    def /(rhs:Int):Op[Raster] = Divide(lhs, Literal(rhs))
    def /(rhs:Op[Int]):Op[Raster] = Divide(lhs, rhs)
  }
  implicit def divideOpRasterBy2(lhs:Op[Raster]) = new {
    def /(rhs:Raster):Op[Raster] = Divide(lhs, Literal(rhs))
    def /(rhs:Op[Raster]):Op[Raster] = Divide(lhs, rhs)
  }

  implicit def divideRasterBy1(lhs:Raster) = new {
    def /(rhs:Int):Op[Raster] = Divide(Literal(lhs), Literal(rhs))
    def /(rhs:Op[Int]):Op[Raster] = Divide(Literal(lhs), rhs)
  }
  implicit def divideRasterBy2(lhs:Raster) = new {
    def /(rhs:Raster):Op[Raster] = Divide(Literal(lhs), Literal(rhs))
    def /(rhs:Op[Raster]):Op[Raster] = Divide(Literal(lhs), rhs)
  }

  implicit def divideOpIntBy1(lhs:Op[Int]) = new {
    def /(rhs:Int):Op[Int] = lhs.map(_ / rhs)
    def /(rhs:Op[Int]):Op[Int] = OpMap2(lhs,rhs).map(_ / _)
  }

  implicit def divideOpIntBy2(lhs:Op[Int]) = new {
    def /(rhs:Raster):Op[Raster] = Divide(lhs, Literal(rhs))
    def /(rhs:Op[Raster]):Op[Raster] = Divide(lhs, rhs)
  }

  implicit def divideIntBy1(lhs:Int) = new {
    def /(rhs:Op[Int]):Op[Int] = rhs.map(lhs / _)
  }
  implicit def divideIntBy2(lhs:Int) = new {
    def /(rhs:Raster):Op[Raster] = Divide(Literal(lhs), Literal(rhs))
    def /(rhs:Op[Raster]):Op[Raster] = Divide(Literal(lhs), rhs)
  }
}

