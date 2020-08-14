/*
 * Copyright 2019 Azavea
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

package geotrellis.util.np

import org.scalatest.matchers.should.Matchers
import org.scalatest.funspec.AnyFunSpec

class PercentileSpec extends AnyFunSpec with Matchers {
  // https://github.com/numpy/numpy/blob/b57957c639ca7c96c328003cc2436a06f8ecf9db/numpy/lib/tests/test_function_base.py#L2508
  describe("percentile should behave like numpy.percentile") {
    it("test basic") {
      val x = (0 until 8).map(_ * 0.5).toArray

      percentile(x, 0) shouldBe 0d
      percentile(x, 100) shouldBe 3.5
      percentile(x, 50) shouldBe 1.75
    }
  }
}