/*
 * Copyright 2016 Azavea
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package geotrellis.util

import org.scalatest._

class ComponentSpec extends FunSpec with Matchers {
  class Upper
  class Middle extends Upper
  class Lower extends Middle

  describe("Identity components") {
    it("should get and set identity") {
      val m = new Middle
      val m2 = new Middle
      m.getComponent[Middle] should be (m)
      m.setComponent[Middle](m2) should be (m2)
    }

    it("should get identity that is subtype") {
      val m = new Middle
      m.getComponent[Upper] should be (m)
    }

    it("should set identity that is supertype") {
      val m = new Middle
      val m2 = new Lower
      m.setComponent[Middle](m2) should be (m2)
    }
  }
}
