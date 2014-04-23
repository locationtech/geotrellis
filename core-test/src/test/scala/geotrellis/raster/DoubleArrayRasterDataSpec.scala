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

package geotrellis.raster

import geotrellis._
import geotrellis.testkit._

import org.scalatest.FunSpec
import org.scalatest.matchers._

class DoubleArrayRasterDataSpec extends FunSpec 
                                  with ShouldMatchers 
                                  with TestServer 
                                  with RasterBuilders {
  describe("DoubleArrayRasterData.toByteArray") {
    it("converts back and forth.") {
      val data = probabilityRaster.asInstanceOf[ArrayRaster].data
      val (cols,rows) = (data.cols,data.rows)
      val data2 = DoubleArrayRasterData.fromArrayByte(data.toArrayByte,cols,rows)
      for(col <- 0 until cols) {
        for(row <- 0 until rows) {
          withClue(s"Values different at ($col,$row)") {
            data2.getDouble(col,row) should be (data.getDouble(col,row))
          }
        }
      }
    }
  }
}
