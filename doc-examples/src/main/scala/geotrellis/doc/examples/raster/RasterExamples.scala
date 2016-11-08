package geotrellis.doc.examples.raster

object RasterExamples {
  def `Counting points in raster cells (raster to vector operation)`: Unit = {
    import geotrellis.raster._
    import geotrellis.vector._

    def countPoints(points: Seq[Point], rasterExtent: RasterExtent): Tile = {
      val (cols, rows) = (rasterExtent.cols, rasterExtent.rows)
      val array = Array.ofDim[Int](cols * rows).fill(0)
      for(point <- points) {
        val x = point.x
        val y = point.y
        if(rasterExtent.extent.intersects(x,y)) {
          val index = rasterExtent.mapXToGrid(x) * cols + rasterExtent.mapYToGrid(y)
          array(index) = array(index) + 1
        }
      }
      IntArrayTile(array, cols, rows)
    }
  }
}
