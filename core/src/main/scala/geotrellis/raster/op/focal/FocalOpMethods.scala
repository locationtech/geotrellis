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
import geotrellis.source._

trait FocalOpMethods[+Repr <: RasterSource] { self: Repr =>
    def zipWithNeighbors:Op[Seq[(Op[Raster],TileNeighbors)]] = 
      (self.tiles,self.rasterDefinition).map { (seq,rd) =>
        val re = rd.rasterExtent
        val tileLayout = rd.tileLayout
        val rl = tileLayout.getResolutionLayout(re)

        val colMax = tileLayout.tileCols - 1
        val rowMax = tileLayout.tileRows - 1

        def getTile(tileCol:Int,tileRow:Int):Option[Op[Raster]] =
          if(0 <= tileCol && tileCol <= colMax &&
            0 <= tileRow && tileRow <= rowMax) {
            Some(seq(tileRow*(colMax+1) + tileCol))
          } else { None }

        seq.zipWithIndex.map { case (tile,i) =>
          val col = i % (colMax+1)
          val row = i / (colMax+1)

          // get tileCols, tileRows, & list of relative neighbor coordinate tuples
          val tileSeq = Seq(
            /* North */
            getTile(col, row - 1),
            /* NorthEast */
            getTile(col + 1, row - 1),
            /* East */
            getTile(col + 1, row),
            /* SouthEast */
            getTile(col + 1, row + 1),
            /* South */
            getTile(col, row + 1),
            /* SouthWest */
            getTile(col - 1, row + 1),
            /* West */
            getTile(col - 1, row),
            /* NorthWest */
            getTile(col - 1, row - 1)
          )

          (tile,SeqTileNeighbors(tileSeq))
        }
      }

  def focal[T,That](n:Neighborhood)
                   (op:(Op[Raster],Op[Neighborhood],TileNeighbors)=>FocalOperation[T])
                   (implicit bf:CanBuildSourceFrom[Repr,T,That]):That = {
    val builder = bf.apply(this)
    
    val newOp = 
      zipWithNeighbors.map(_.map { case (t,ns) => op(t,n,ns) })

    builder.setOp(newOp)
    val result = builder.result()
    result
  }

  def focalSum(n:Neighborhood) = focal(n)(Sum(_,_,_))

  def focalMin(n:Neighborhood) = focal(n)(Min(_,_,_))
  def focalMax(n:Neighborhood) = focal(n)(Max(_,_,_))

  def focalMean(n:Neighborhood) = focal(n)(Mean(_,_,_))
  def focalMedian(n:Neighborhood) = focal(n)(Median(_,_,_))
  def focalMode(n:Neighborhood) = focal(n)(Mode(_,_,_))
  def focalStandardDeviation(n:Neighborhood) = focal(n)(StandardDeviation(_,_,_))

  def focalAspect = focal(Square(1))((r,_,nbs) => Aspect(r,nbs))
  def focalSlope = focal(Square(1))((r,_,nbs) => Slope(r,nbs))
  def focalHillshade = focal(Square(1))((r,_,nbs) => Hillshade(r,nbs))
  def focalHillshade(azimuth:Double,altitude:Double,zFactor:Double) =
    focal(Square(1))((r,_,nbs) => Hillshade(r,nbs,azimuth,altitude,zFactor))

  def focalMoransI(n:Neighborhood) =
    self.globalOp(RasterMoransI(_,n))

  def focalScalarMoransI(n:Neighborhood) =
    self.converge.mapOp(ScalarMoransI(_,n))
}
