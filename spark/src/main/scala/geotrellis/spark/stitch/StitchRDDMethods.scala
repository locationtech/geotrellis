package geotrellis.spark.stitch

import geotrellis.raster._
import geotrellis.raster.stitch.{Stitcher, StitcherR}
import geotrellis.vector.Extent
import geotrellis.spark._
import geotrellis.spark.tiling.MapKeyTransform
import org.apache.spark.rdd.RDD

object TileLayoutStitcher {
  /**
   * Stitches a collection of tiles keyed by (col, row) tuple into a single tile.
   * Makes the assumption that all tiles are of equal size such that they can be placed in a grid layout.
   * @return An option of stitched tile and the GridBounds of keys used to construct it.
   */
  def stitch[V <: CellGrid, R <: CellGrid](tiles: Iterable[(Product2[Int, Int], V)])
  (implicit stitcher: StitcherR[V, R]): (R, GridBounds) = {
    require(tiles.nonEmpty, "nonEmpty input")
    val sample = tiles.head._2
    val te = GridBounds.envelope(tiles.map(_._1))
    val tileCols = sample.cols
    val tileRows = sample.rows

    val pieces =
      for ((SpatialKey(col, row), v) <- tiles) yield {
        val updateCol = (col - te.colMin) * tileCols
        val updateRow = (row - te.rowMin) * tileRows
        (v, (updateCol, updateRow))
      }
    val tile: R = stitcher.stitch(pieces, te.width * tileCols, te.height * tileRows)
    (tile, te)
  }
}

abstract class SpatialTileLayoutRDDMethods[V <: CellGrid, M: ? => MapKeyTransform]
  extends MethodExtensions[RDD[(SpatialKey, V)] with Metadata[M]] {

  def stitch[R <: CellGrid](implicit stitcher: StitcherR[V, R]): Raster[R] = {
    val (tile, bounds) = TileLayoutStitcher.stitch(self.collect())
    val mkt: MapKeyTransform = self.metadata
    Raster(tile, mkt(bounds))
  }
}

abstract class SpatialTileRDDMethods[V <: CellGrid]
  extends MethodExtensions[RDD[(SpatialKey, V)]] {

  def stitch[R <: CellGrid](implicit stitcher: StitcherR[V, R]): R = {
    TileLayoutStitcher.stitch(self.collect())._1
  }
}