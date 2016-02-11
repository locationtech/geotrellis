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

package geotrellis.raster.render

import org.scalatest._


import java.util.Locale

class ColorClassifierSpec extends FunSpec with Matchers {
  describe("construction") {
    it("should build a mapping from doubles to colors") {
      val cc = new StrictIntColorClassifier
      cc.classify(123, RGBA(123))
        .classify(1234, RGBA(1234))
        .classify(1235, RGBA(1235))
        .classify(1236, RGBA(1236))
        .setNoDataColor(RGBA(8675309))
      cc.getColors shouldBe (Array(RGBA(123), RGBA(1234), RGBA(1235), RGBA(1236)))
      cc.getBreaks shouldBe (Array(123.0, 1234.0, 1235.0, 1236.0))
      cc.getNoDataColor shouldBe (Some(RGBA(8675309)))
    }
  }
}
