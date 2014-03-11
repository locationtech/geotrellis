/***
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
 ***/

package geotrellis

import org.scalatest.FunSuite
import org.scalatest.matchers.ShouldMatchers
import geotrellis.testkit._
import geotrellis.raster.op._


class AndThenTest extends FunSuite 
                     with ShouldMatchers 
                     with TestServer {
  test("AndThen should forward operations forward") {
    case class TestOp1(x:Int) extends Op1(x)({x:Int => AndThen(x + 4)})

    val r = get(TestOp1(3))
    assert(r == 7) 
  }
}
