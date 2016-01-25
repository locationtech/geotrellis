package geotrellis.raster.stitch

import geotrellis.raster._

trait Stitcher[T <: CellGrid] extends Serializable {
  def stitch(pieces: Iterable[(T, (Int, Int))], cols: Int, rows: Int): T
}

object Stitcher {
  implicit def stitcher[T <: Tile]: Stitcher[T] =
    TileStitcher

  implicit object TileStitcher[T <: Tile] extends Stitcher[T] {
    def stitch(pieces: Iterable[(T, (Int, Int))], cols: Int, rows: Int): Tile = {
      val result = ArrayTile.empty(pieces.head._1.cellType, cols, rows)
      for((tile, (updateColMin, updateRowMin)) <- pieces) {
        if(updateColMin + tile.cols > cols || updateRowMin + tile.rows > rows) {
          sys.error(s"Invalid stitch call. Target cols, rows: ${(cols, rows)}. Piece cols, rows: ${tile.dimensions}. Update ${(updateColMin, updateRowMin)} Pieces: ${pieces.size}")
        }
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
