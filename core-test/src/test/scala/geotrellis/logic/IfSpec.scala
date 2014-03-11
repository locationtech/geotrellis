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

package geotrellis.logic

import geotrellis._
import geotrellis.testkit._

import org.scalatest.FunSpec
import org.scalatest.matchers.ShouldMatchers

class IfSpec extends FunSpec 
                with TestServer
                with ShouldMatchers {
  describe("the if operation should only eval 1 argument") {
    val ErrorOp = op { (x:Int) => { sys.error("execute this op"); x } }

    val result1 = get(If(Literal(true), Literal(1), ErrorOp(2)))
    result1 should be (1)

    val result2 = get(If(Literal(false), ErrorOp(1), Literal(2)))
    result2 should be (2)
  }
}
