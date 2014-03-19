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

package geotrellis.logic

import geotrellis._
import geotrellis.testkit._

import org.scalatest.FunSpec
import org.scalatest.matchers.ShouldMatchers

class FilterSpec extends FunSpec 
                    with TestServer
                    with ShouldMatchers {
  describe("FilterSpec") {
    it("should filter Op[Seq[Int]] with Int to Boolean function") {
      val seq = Literal(Seq(1,2,3,4,5,6,7,8,9,10))
      val result = get(Filter(seq, {i:Int => i % 2 == 0}))
      result should be (Seq(2,4,6,8,10))
    }
    it("should filter Op[Seq[Int]] with int to Op[Boolean] function") {
      val seq = Literal(Seq(1,2,3,4,5,6,7,8,9,10))
      val result = get(Filter(seq, { i:Int =>  Literal(i+1).map(_ % 2 == 0) }))
      result should be (Seq(1,3,5,7,9))
    }
  }
}
