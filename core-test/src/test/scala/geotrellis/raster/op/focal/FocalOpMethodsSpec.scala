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

package geotrellis.raster.op.focal

import geotrellis._
import geotrellis.raster._
import geotrellis.testkit._
import geotrellis.source._
import geotrellis.process._

import org.scalatest.FunSpec

class FocalOpMethodsSpec extends FunSpec with FocalOpSpec 
                                         with TestServer {
  describe("zipWithNeighbors") {
    it("Gets correct neighbors for 3 x 2 tiles") {
      val rs1 = createRasterSource(
        Array( 1,1,1,      2,2,2,      3,3,3,
               1,1,1,      2,2,2,      3,3,3,

               4,4,4,      5,5,5,      6,6,6,
               4,4,4,      5,5,5,      6,6,6,

               7,7,7,      8,8,8,      9,9,9,
               7,7,7,      8,8,8,      9,9,9,

              10,10,10,   11,11,11,   12,12,12,
              10,10,10,   11,11,11,   12,12,12
        ),
        3,4,3,2
      )

      val neighborsOp:Op[Seq[(Raster,Seq[Option[Raster]])]] = 
        rs1.zipWithNeighbors
           .map { seq =>
              seq.map { case (rOp,tileNeighbors) =>
                   (rOp,tileNeighbors.getNeighbors).map { (_,_) }
                  }
             }
           .collect

      for((r,neighbors) <- get(neighborsOp)) {
        r.get(0,0) match {
          case 1 =>
            withClue("NW") {
              neighbors.flatten.map(_.get(0,0)).sorted should be (Seq(2,4,5))
            }
          case 2 =>
            withClue("N") {
              neighbors.flatten.map(_.get(0,0)).sorted should be (Seq(1,3,4,5,6))
            }
          case 3 =>
            withClue("NE") {
              neighbors.flatten.map(_.get(0,0)).sorted should be (Seq(2,5,6))
            }
          case 4 =>
            withClue("W") {
              neighbors.flatten.map(_.get(0,0)).sorted should be (Seq(1,2,5,7,8))
            }
          case 5 =>
            withClue("CENTER") {
              neighbors.flatten.map(_.get(0,0)).sorted should be (Seq(1,2,3,4,6,7,8,9))
            }
          case 6 =>
            withClue("E") {
              neighbors.flatten.map(_.get(0,0)).sorted should be (Seq(2,3,5,8,9))
            }
          case 7 =>
            withClue("SW") {
              neighbors.flatten.map(_.get(0,0)).sorted should be (Seq(4,5,8,10,11))
            }
          case 8 =>
            withClue("S") {
              neighbors.flatten.map(_.get(0,0)).sorted should be (Seq(4,5,6,7,9,10,11,12))
            }
          case 9 =>
            withClue("SE") {
              neighbors.flatten.map(_.get(0,0)).sorted should be (Seq(5,6,8,11,12))
            }
          case 10 =>
            withClue("LASTROW West") {
              neighbors.flatten.map(_.get(0,0)).sorted should be (Seq(7,8,11))
            }
          case 11 =>
            withClue("LASTROW Center") {
              neighbors.flatten.map(_.get(0,0)).sorted should be (Seq(7,8,9,10,12))
            }
          case 12 =>
            withClue("LASTROW East") {
              neighbors.flatten.map(_.get(0,0)).sorted should be (Seq(8,9,11))
            }
          case _ =>
            sys.error("erm...wha?")
        }
      }
    }
  }
}
