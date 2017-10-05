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

class HaversineSpec extends FunSpec with Matchers {
  describe("HaversineSpec") {
    // Rio de Janeiro
    val rio = -22.906847 -> -43.172896
    // São Paulo
    val sp = -23.550520 -> -46.633309

    it("should calculate distance between Rio de Janeiro and São Paulo") {
      (Haversine(rio, sp) / 1000).toInt shouldBe 388
    }
  }
}
