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

package geotrellis.raster.op.focal

import geotrellis._
import geotrellis.process._
import geotrellis.raster.op._
import geotrellis.testkit._

import org.scalatest.FunSpec
import org.scalatest.matchers._

import scala.math._

class SlopeAspectTests extends FunSpec 
                          with ShouldMatchers 
                          with TestServer {
  describe("SurfacePoint") {
    it("should calculate trig values correctly") {
      val tolerance = 0.0000000001
      for(x <- Seq(-1.2,0,0.1,2.4,6.4,8.1,9.8)) {
        for(y <- Seq(-11,-3.2,0,0.3,2.65,3.9,10.4,8.11)) {
          val s = new SurfacePoint
          s.`dz/dx` = x
          s.`dz/dy` = y
          val aspect = s.aspect
          val slope = s.slope
          
          if(isData(slope)) {
            abs(s.cosSlope - cos(slope)) should be < tolerance
            abs(s.sinSlope - sin(slope)) should be < tolerance
          }
          if(isData(aspect)) {
            abs(s.sinAspect - sin(aspect)) should be < tolerance
            abs(s.cosAspect - cos(aspect)) should be < tolerance
          }
        }
      }
    }
  }
}

