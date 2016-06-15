package geotrellis.raster

import geotrellis.raster.rasterize.polygon._
import geotrellis.vector._
import geotrellis.vector.voronoi._
import scala.math.sqrt

object EuclideanDistanceTile {

  def apply(pts: Array[Point], re: RasterExtent): Tile = {
    val vor = new Voronoi(pts, re.extent)
    val tile = DoubleArrayTile.empty(re.cols, re.rows)
    
    def fillFn(base: Point)(c: Int, r: Int): Unit = {
      val (x,y) = re.gridToMap(c,r)
      tile.setDouble(c,r,sqrt((x-base.x)*(x-base.x) + (y-base.y)*(y-base.y)))
    }

    def rasterizeDistanceCell(arg: (Polygon, Point)) = {
      val (poly, pt) = arg
      
      PolygonRasterizer.foreachCellByPolygon(poly, re)(fillFn(pt))
    }
    vor.voronoiCellsWithPoints.foreach(rasterizeDistanceCell)

    tile
  }

}
