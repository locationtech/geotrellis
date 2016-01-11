package geotrellis.raster.stitch

import geotrellis.raster._

trait Stitcher[T <: CellGrid] extends Serializable {
  def stitch(pieces: Iterable[(T, (Int, Int))], cols: Int, rows: Int): T
}

object Stitcher {
  implicit object TileStitcher extends Stitcher[Tile] {
    def stitch(pieces: Iterable[(Tile, (Int, Int))], cols: Int, rows: Int): Tile = {
      val result = ArrayTile.empty(pieces.head._1.cellType, cols, rows)
      for((tile, (updateColMin, updateRowMin)) <- pieces) {
        result.update(updateColMin, updateRowMin, tile)
      }
      result
    }
  }

  implicit object MultiBandTileStitcher extends Stitcher[MultiBandTile] {
    def stitch(pieces: Iterable[(MultiBandTile, (Int, Int))], cols: Int, rows: Int): MultiBandTile = {
      val headTile = pieces.head._1
      val bands = Array.fill[MutableArrayTile](headTile.bandCount)(ArrayTile.empty(headTile.cellType, cols, rows))

      for ((tile, (updateColMin, updateRowMin)) <- pieces) {
        for(b <- 0 until headTile.bandCount) {
          bands(b).update(updateColMin, updateRowMin, tile.band(b))
        }
      }

      ArrayMultiBandTile(bands)
    }
  }

}
