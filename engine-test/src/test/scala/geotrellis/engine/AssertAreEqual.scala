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

package geotrellis.engine

import geotrellis.raster._
import geotrellis.raster.op.local._

import scala.math._

import org.scalatest.matchers._

object AssertAreEqual {
  def apply(r1: Op[Tile], r2: Op[Tile], threshold: Double) = {
    (r1, r2).map(_.dualCombine(_)((z1: Int, z2: Int) => {
        println(s"${z1}")
          if (isNoData(z1)) {
            if(isData(z2))
              sys.error(s"AssertEqual failed: MISMATCH z1 = ${z1}  z2 = ${z2}")
            0
          } else if (isNoData(z2)) {
            sys.error(s"AssertEqual failed: MISMATCH z1 = ${z1}  z2 = ${z2}")
          } else {
            if(abs(z1 - z2) > threshold)
              sys.error(s"AssertEqual failed: MISMATCH z1 = ${z1}  z2 = ${z2}")
            0
          }
    })((z1: Double, z2: Double) => {
        if (isNoData(z1)) {
          if(isData(z2))
            sys.error(s"AssertEqual failed: MISMATCH z1 = ${z1}  z2 = ${z2}")
          0.0
        } else if (isNoData(z2)) {
          sys.error(s"AssertEqual failed: MISMATCH z1 = ${z1}  z2 = ${z2}")
        } else {
          if(abs(z1 - z2) > threshold)
            sys.error(s"AssertEqual: MISMATCH z1 = ${z1}  z2 = ${z2}")
          0.0
        }
      })
    )
  }
}