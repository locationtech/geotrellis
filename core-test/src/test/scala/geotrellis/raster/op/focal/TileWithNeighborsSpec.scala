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

package geotrellis.raster.op.focal

import geotrellis._
import geotrellis.raster._
import geotrellis.testkit._
import geotrellis.source._
import geotrellis.process._

import org.scalatest.FunSpec

class TileWithNeighborsSpec extends FunSpec with FocalOpSpec 
                                            with TestServer {
  describe("TileWithNeighbors") {
    it("should tile correctly for 3x2 columns") {
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

      val tiledRasters =
        get(
          rs1.zipWithNeighbors
             .map { seq =>
               seq.map { case (rOp,tileNeighbors) =>
               (rOp,tileNeighbors.getNeighbors).map { (_,_) }
              }
             }
            .collect
        )
        .map { case (r,neighbors) => 
            val twn = TileWithNeighbors(r,neighbors)
            (r,neighbors,twn._1,twn._2)
        }

      for((r,neighbors,tiledRaster,analysisArea) <- tiledRasters) {
        r.get(0,0) match {
          case 1 =>
            withClue("NW") {
              assertEqual(tiledRaster,
                Array( 1,1,1,  2,2,2,
                       1,1,1,  2,2,2,
                       
                       4,4,4,  5,5,5,
                       4,4,4,  5,5,5)
              )
              analysisArea should be (GridBounds(0,0,2,1))
            }
          case 2 =>
            withClue("N") {
              assertEqual(tiledRaster,
                Array( 1,1,1,  2,2,2,  3,3,3,
                       1,1,1,  2,2,2,  3,3,3,
                       
                       4,4,4,  5,5,5,  6,6,6,
                       4,4,4,  5,5,5,  6,6,6)
              )
              analysisArea should be (GridBounds(3,0,5,1))
            }
          case 3 =>
            withClue("NE") {
              assertEqual(tiledRaster,
                Array( 2,2,2,  3,3,3,
                       2,2,2,  3,3,3,
                       
                       5,5,5,  6,6,6,
                       5,5,5,  6,6,6)
              )
              analysisArea should be (GridBounds(3,0,5,1))
            }
          case 4 =>
            withClue("W") {
              assertEqual(tiledRaster,
                Array( 1,1,1,  2,2,2,
                       1,1,1,  2,2,2,
                       
                       4,4,4,  5,5,5,
                       4,4,4,  5,5,5,

                       7,7,7,  8,8,8,
                       7,7,7,  8,8,8
                )
              )
              analysisArea should be (GridBounds(0,2,2,3))
            }
          case 5 =>
            withClue("CENTER") {
              assertEqual(tiledRaster,
                Array( 1,1,1,  2,2,2,  3,3,3,
                       1,1,1,  2,2,2,  3,3,3,
                       
                       4,4,4,  5,5,5,  6,6,6,
                       4,4,4,  5,5,5,  6,6,6,

                       7,7,7,  8,8,8,  9,9,9,
                       7,7,7,  8,8,8,  9,9,9
                )
              )
              analysisArea should be (GridBounds(3,2,5,3))
            }
          case 6 =>
            withClue("E") {
              assertEqual(tiledRaster,
                Array( 2,2,2,  3,3,3,
                       2,2,2,  3,3,3,
                       
                       5,5,5,  6,6,6,
                       5,5,5,  6,6,6,

                       8,8,8,  9,9,9,
                       8,8,8,  9,9,9
                )
              )
              analysisArea should be (GridBounds(3,2,5,3))
            }
          case 7 =>
            withClue("SW") {
              assertEqual(tiledRaster,
                Array( 4,4,4,      5,5,5,
                       4,4,4,      5,5,5,

                       7,7,7,      8,8,8,
                       7,7,7,      8,8,8,

                      10,10,10,   11,11,11,
                      10,10,10,   11,11,11
                )
              )
              analysisArea should be (GridBounds(0,2,2,3))
            }
          case 8 =>
            withClue("S") {
              assertEqual(tiledRaster,
                Array( 4,4,4,      5,5,5,      6,6,6,
                       4,4,4,      5,5,5,      6,6,6,

                       7,7,7,      8,8,8,      9,9,9,
                       7,7,7,      8,8,8,      9,9,9,

                      10,10,10,   11,11,11,   12,12,12,
                      10,10,10,   11,11,11,   12,12,12
                )
              )
              analysisArea should be (GridBounds(3,2,5,3))
            }
          case 9 =>
            withClue("SE") {
              assertEqual(tiledRaster,
                Array( 5,5,5,      6,6,6,
                       5,5,5,      6,6,6,

                       8,8,8,      9,9,9,
                       8,8,8,      9,9,9,

                      11,11,11,   12,12,12,
                      11,11,11,   12,12,12
                )
              )
              analysisArea should be (GridBounds(3,2,5,3))
            }
          case 10 =>
            withClue("LASTROW West") {
              assertEqual(tiledRaster,
                Array( 7,7,7,      8,8,8,
                       7,7,7,      8,8,8,

                      10,10,10,   11,11,11,
                      10,10,10,   11,11,11
                )
              )
              analysisArea should be (GridBounds(0,2,2,3))
            }
          case 11 =>
            withClue("LASTROW Center") {
              assertEqual(tiledRaster,
                Array( 7,7,7,      8,8,8,      9,9,9,
                       7,7,7,      8,8,8,      9,9,9,

                      10,10,10,   11,11,11,   12,12,12,
                      10,10,10,   11,11,11,   12,12,12
                )
              )
              analysisArea should be (GridBounds(3,2,5,3))
            }
          case 12 =>
            withClue("LASTROW East") {
              assertEqual(tiledRaster,
                Array( 8,8,8,      9,9,9,
                       8,8,8,      9,9,9,

                      11,11,11,   12,12,12,
                      11,11,11,   12,12,12
                )
              )
              analysisArea should be (GridBounds(3,2,5,3))
            }
          case _ =>
            sys.error("Test error.")
        }
      }
    }

    it("should tile correctly for an untiled raster") {
      val e = Extent(0.0, 0.0, 4.0, 4.0)
      val re = RasterExtent(e, 1.0, 1.0, 4, 4)
      val r = Raster(IntConstant(1, 4, 4), re)
      val (tiled,analysisArea) = 
        TileWithNeighbors(r,Seq[Option[Raster]]())
      analysisArea should be (GridBounds(0,0,3,3))
      assertEqual(r,tiled)
    }
  }
}
