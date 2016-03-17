package geotrellis.raster.summary.polygonal

import geotrellis.raster._
import geotrellis.raster.rasterize._
import geotrellis.vector._


/**
  * Object containing functions for doing sum operations on
  * [[Raster]]s.
  */
object SumSummary extends TilePolygonalSummaryHandler[Long] {

  /**
    * Given a [[Raster]] which partially intersects the given polygon,
    * find the sum of the Raster elements in the intersection.
    */
  def handlePartialTile(raster: Raster[Tile], polygon: Polygon): Long = {
    val Raster(tile, _) = raster
    val rasterExtent = raster.rasterExtent
    var sum: Long = 0L

    polygon.foreach(rasterExtent)({ (col: Int, row: Int) =>
      val z = tile.get(col, row)
      if (isData(z)) { sum = sum + z }
    })

    sum
  }

  /**
    * Find the sum of the elements in the [[Raster]].
    */
  def handleFullTile(tile: Tile): Long = {
    var s = 0L
    tile.foreach { (x: Int) => if (isData(x)) s = s + x }
    s
  }

  /**
    * Combine the results into a larger result.
    */
  def combineResults(rs: Seq[Long]) =
    rs.foldLeft(0L)(_+_)
}

/**
  * Object containing functions for doing sum operations on
  * [[Raster]]s.
  */
object SumDoubleSummary extends TilePolygonalSummaryHandler[Double] {

  /**
    * Given a [[Raster]] which partially intersects the given polygon,
    * find the sum of the Raster elements in the intersection.
    */
  def handlePartialTile(raster: Raster[Tile], polygon: Polygon): Double = {
    val Raster(tile, _) = raster
    val rasterExtent = raster.rasterExtent
    var sum = 0.0

    polygon.foreach(rasterExtent)({ (col: Int, row: Int) =>
      val z = tile.getDouble(col, row)
      if(isData(z)) { sum = sum + z }
    })

    sum
  }

  /**
    * Find the sum of the elements in the [[Raster]].
    */
  def handleFullTile(tile: Tile): Double = {
    var s = 0.0
    tile.foreachDouble((x: Double) => if (isData(x)) s = s + x)
    s
  }

  /**
    * Combine the results into a larger result.
    */
  def combineResults(rs: Seq[Double]) =
    rs.foldLeft(0.0)(_+_)
}
