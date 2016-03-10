package geotrellis.spark.stitch

import geotrellis.raster._
import geotrellis.raster.stitch.Stitcher
import geotrellis.vector.Extent
import geotrellis.spark._
import geotrellis.spark.tiling.MapKeyTransform
import geotrellis.util.MethodExtensions

import org.apache.spark.rdd.RDD


object TileLayoutStitcher {
  /**
   * Stitches a collection of tiles keyed by (col, row) tuple into a single tile.
   * Makes the assumption that all tiles are of equal size such that they can be placed in a grid layout.
   * @return An option of stitched tile and the GridBounds of keys used to construct it.
   */
  def stitch[V <: CellGrid](tiles: Iterable[(Product2[Int, Int], V)])
  (implicit stitcher: Stitcher[V]): (V, GridBounds) = {
    require(tiles.nonEmpty, "nonEmpty input")
    val sample = tiles.head._2
    val te = GridBounds.envelope(tiles.map(_._1))
    val tileCols = sample.cols
    val tileRows = sample.rows

    val pieces =
      for ((GridKey(col, row), v) <- tiles) yield {
        val updateCol = (col - te.colMin) * tileCols
        val updateRow = (row - te.rowMin) * tileRows
        (v, (updateCol, updateRow))
      }
    val tile: V = stitcher.stitch(pieces, te.width * tileCols, te.height * tileRows)
    (tile, te)
  }
}

abstract class SpatialTileLayoutRDDMethods[V <: CellGrid, M: ? => MapKeyTransform]
  extends MethodExtensions[RDD[(GridKey, V)] with Metadata[M]] {

  def stitch(implicit stitcher: Stitcher[V]): Raster[V] = {
    val (tile, bounds) = TileLayoutStitcher.stitch(self.collect())
    val mkt: MapKeyTransform = self.metadata
    Raster(tile, mkt(bounds))
  }
}

abstract class SpatialTileRDDMethods[V <: CellGrid]
  extends MethodExtensions[RDD[(GridKey, V)]] {

  def stitch(implicit stitcher: Stitcher[V]): V = {
    TileLayoutStitcher.stitch(self.collect())._1
  }
}
