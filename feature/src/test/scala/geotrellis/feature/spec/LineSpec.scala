/**************************************************************************
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
 **************************************************************************/

package geotrellis.feature.spec

import geotrellis.feature._

import com.vividsolutions.jts.{geom=>jts}

import org.scalatest.FunSpec
import org.scalatest.matchers._

class LineSpec extends FunSpec with ShouldMatchers {
  describe("Line") {
    it("should be a closed Line if constructed with l(0) == l(-1)") {
      val l = Line(List[(Double,Double)]((0,0),(1,0),(1,1),(0,1),(0,0)))
      l.isClosed should be (true)
    }

    it("should return true for crosses for MultiLine it crosses") {
      val l = Line( (0.0, 0.0), (5.0, 5.0) )
      val ml = 
        MultiLine (
          Line( (1.0, 0.0), (1.0, 5.0) ),
          Line( (2.0, 0.0), (2.0, 5.0) ),
          Line( (3.0, 0.0), (3.0, 5.0) ),
          Line( (4.0, 0.0), (4.0, 5.0) )
        )

      l.crosses(ml) should be (true)
    }
  }
}
