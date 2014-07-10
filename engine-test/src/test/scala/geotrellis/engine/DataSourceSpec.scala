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

package geotrellis.engine

import geotrellis.raster.op._
import geotrellis.testkit._

import org.scalatest._

class DataSourceSpec extends FunSpec 
                        with Matchers 
                        with TestEngine 
                        with TileBuilders {
  describe("DataSource") {
    it("should cache") {
      case class MockOperation(f:()=>Unit) extends Op1(f)({ f => f(); Result(true) })

      val wasRan = Array.fill[Int](4)(0)
      val mockOps = 
        (0 until 4).map { i =>
          MockOperation { () => wasRan(i) += 1 } 
        }

      val ds = SeqSource(mockOps.toSeq)
      wasRan should be (Array(0,0,0,0))
      val dsCached = ds.cached
      wasRan should be (Array(1,1,1,1))
      dsCached.run match {
        case Complete(v,_) =>
          v.toArray should be (Array(true,true,true,true))
          wasRan should be (Array(1,1,1,1))
        case Error(msg,_) =>
          sys.error(msg)
      }
    }
  }

  describe("fromSources") {
    it("should combine sources") {
      val seq = Seq(
        ValueSource(1),
        ValueSource(2),
        ValueSource(3),
        DataSource.fromValues(1,2,1).reduce(_+_)
      )
      seq.get.toArray should be (Array(1,2,3,4))
    }

    it("should implicitly have collectSources") {
      val seq = Seq(
        ValueSource(1),
        ValueSource(2),
        ValueSource(3),
        DataSource.fromValues(1,2,1).reduce(_+_)
      )
      seq.collectSources.get.toArray should be (Array(1,2,3,4))
    }

    it("should implicitly have collectSources for ValueSource sequence") {
      val seq: Seq[ValueSource[Int]] = Seq(
        ValueSource(1),
        ValueSource(2),
        ValueSource(3)
      )
      seq.collectSources.get.toArray should be (Array(1,2,3))
    }
  }
}
