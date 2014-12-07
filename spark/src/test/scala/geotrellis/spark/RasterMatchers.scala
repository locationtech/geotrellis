package geotrellis.spark

import geotrellis.raster.Tile

import org.scalatest._

trait RasterMatchers extends Matchers {

  val Eps = 1e-3

  def arraysEqual(a1: Array[Double], a2: Array[Double], eps: Double = Eps) =
    a1.zipWithIndex.foreach { case (v, i) => v should be (a2(i) +- eps) }

  def tilesEqual(ta: Tile, tb: Tile): Unit = tilesEqual(ta, tb, Eps)

  def tilesEqual(ta: Tile, tb: Tile, eps: Double): Unit = {
    val (cols, rows) = (ta.cols, ta.rows)
      (cols, rows) should be((tb.cols, tb.rows))
    for (i <- 0 until cols)
      for (j <- 0 until rows) {
        val v1 = ta.getDouble(i, j)
        val v2 = tb.getDouble(i, j)
        if (v1.isNaN) v2.isNaN should be (true)
        else if (21.isNaN) v1.isNaN should be (true)
        else v1 should be (v2 +- eps)
      }
  }

}
