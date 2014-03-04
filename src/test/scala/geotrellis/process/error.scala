/*******************************************************************************
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
 ******************************************************************************/

package geotrellis.process

import org.scalatest.FunSpec
import org.scalatest.matchers.MustMatchers

import geotrellis._
import geotrellis.process._
import geotrellis.testutil._

case class Defenestrator(msg:String) extends Operation[Unit] {
  def _run() = {
    throw new Exception(msg)
    Result(Unit)
  }

  val nextSteps:Steps = { case _ => Result(Unit)}
}

case class Window(msg:String) extends Op[Unit] {
  def _run() = {
    runAsync(List(Defenestrator(msg)))
  }
  val nextSteps:Steps = {
    case _ => {
      throw new Exception("we should never arrive here")
      Result(Unit)
    }
  }
}

case class LargeWindow(a:Op[Unit], b:Op[Unit]) extends Op2(a,b) ({
  (a,b) => {
    throw new Exception("leaping from large window")
    Result(Unit) 
  }
})

class FailingOpSpec extends FunSpec 
                       with MustMatchers 
                       with TestServer {

  describe("A failing operation") {
    it("should return a StepError") {
      val op = Defenestrator("Ermintrude Inch")
      val op2 = Window("extremely high")
      val op3 = LargeWindow(Literal(Unit), op2)
      evaluating { get(op) } must produce [java.lang.RuntimeException]
      evaluating { get(op2) } must produce [java.lang.RuntimeException]
      evaluating { get(op3) } must produce [java.lang.RuntimeException]
    }
  }
}
