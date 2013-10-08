package geotrellis.raster.op.focal

import geotrellis._
import geotrellis.raster._
import geotrellis.source._

trait FocalOpMethods[+Repr <: RasterSource] { self: Repr =>
    def zipWithNeighbors:Op[Seq[(Op[Raster],TileNeighbors)]] = {
    (self.tiles,self.rasterDefinition).map { (seq,rd) =>
      val re = rd.re
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
        val col = i % re.cols
        val row = i / re.rows

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
  }

  def focal[T,That](n:Neighborhood)
                   (op:(Op[Raster],Op[Neighborhood],TileNeighbors)=>FocalOperation[T])
                   (implicit bf:CanBuildSourceFrom[Repr,T,That]):That = {
    val builder = bf.apply(this)
    
    val newOp = 
      zipWithNeighbors.map(seq => seq.map { case (t,ns) => op(t,n,ns) })

    builder.setOp(newOp)
    val result = builder.result()
    result
  }

  def focalMin(n:Neighborhood) = focal(n)(Min(_,_,_))
  def focalMax(n:Neighborhood) = focal(n)(Max(_,_,_))
}
