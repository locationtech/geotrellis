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

package geotrellis.util

import org.scalatest.FunSpec
import org.scalatest.matchers.MustMatchers

class UtilSpec extends FunSpec with MustMatchers {
  describe("Util package") {
    it("implements time()") {
      val (n, t) = Timer.time { Thread.sleep(100); 99 }
      n must be === 99
      t >= 100 must be === true
    }

    it("implements run()") {
      val n = Timer.run { 99 }
      n must be === 99
    }

    it("implements log()") {
      val i = 3
      val n = Timer.log("msg(%d)", i) { 99 }
      n must be === 99
    }
  }
}

