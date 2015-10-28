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

import geotrellis.raster._

object TileWithNeighbors {
  def apply(r: Tile, neighbors: Seq[Option[Tile]]): (Tile, GridBounds) = 
    if(neighbors.isEmpty) {
      (r, GridBounds(0, 0, r.cols-1, r.rows-1))
    } else {

      val nw = neighbors(7)
      val n = neighbors(0)
      val ne = neighbors(1)
      val w = neighbors(6)
      val e = neighbors(2)
      val sw = neighbors(5)
      val s = neighbors(4)
      val se = neighbors(3)

      val westCol = 
        if(Seq(nw, w, sw).flatten.isEmpty) 0 else 1
      val eastCol = 
        if(Seq(ne, e, se).flatten.isEmpty) 0 else 1
      val northRow = 
        if(Seq(nw, n, ne).flatten.isEmpty) 0 else 1
      val southRow = 
        if(Seq(sw, s, se).flatten.isEmpty) 0 else 1

      val tileCols = 1 + westCol + eastCol
      val tileRows = 1 + northRow + southRow

      val tileLayout = TileLayout(tileCols, tileRows, r.cols, r.rows)

      // Determine the min/max index of the target raster inside of the
      // tiled raster (the analysis area).
      val colMin = if(westCol == 0) 0 else (neighbors(6).get.cols)
      val colMax = colMin + r.cols - 1
      val rowMin = if(northRow == 0) 0 else (neighbors(0).get.rows)
      val rowMax = rowMin + r.rows - 1

      val tiledTile = 
        CompositeTile(Seq(
          neighbors(7),
          neighbors(0),
          neighbors(1),
          neighbors(6),
          Some(r),
          neighbors(2),
          neighbors(5),
          neighbors(4),
          neighbors(3)
        ).flatten, tileLayout)
      (tiledTile, GridBounds(colMin, rowMin, colMax, rowMax))
    }

  def fromOffsets(pairs: TraversableOnce[((Int, Int), Tile)]): Option[(Tile, GridBounds)]  = {
    val seq = Array.fill[Option[Tile]](8)(None)  
    var center: Option[Tile] = None
    
    pairs.foreach { case ((dCol, dRow), tile) =>
      if (dRow == 0 && dCol == 0) center = Some(tile)
      else if (dRow == -1 && dCol ==  0) seq(0) = Some(tile)
      else if (dRow == -1 && dCol ==  1) seq(1) = Some(tile)
      else if (dRow ==  0 && dCol ==  1) seq(2) = Some(tile)
      else if (dRow ==  1 && dCol ==  1) seq(3) = Some(tile)
      else if (dRow ==  1 && dCol ==  0) seq(4) = Some(tile)
      else if (dRow ==  1 && dCol == -1) seq(5) = Some(tile)
      else if (dRow ==  0 && dCol == -1) seq(6) = Some(tile)
      else if (dRow == -1 && dCol == -1) seq(7) = Some(tile)
    }

    // if we did not find center, that means we contributed to tile that is not in the source collection
    center.map { tile =>
      TileWithNeighbors(tile, seq)
    }    
  }  
}
