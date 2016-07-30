package geotrellis.raster.distance

import geotrellis.raster.{RasterExtent, DoubleArrayTile, Tile}
import geotrellis.raster.rasterize.polygon.PolygonRasterizer
import geotrellis.vector.{Point, Polygon}
import geotrellis.vector.voronoi._
import scala.math.sqrt

object EuclideanDistanceTile {

  def apply(pts: Array[Point], rasterExtent: RasterExtent): Tile = {
    val vor = pts.toList.voronoiDiagram(rasterExtent.extent)
    val tile = DoubleArrayTile.empty(rasterExtent.cols, rasterExtent.rows)
    
    def fillFn(base: Point)(col: Int, row: Int): Unit = {
      val (x,y) = rasterExtent.gridToMap(col, row)
      tile.setDouble(col, row, sqrt((x - base.x) * (x - base.x) + (y - base.y) * (y - base.y)))
    }

    def rasterizeDistanceCell(arg: (Polygon, Point)) = {
      val (poly, pt) = arg
      
      PolygonRasterizer.foreachCellByPolygon(poly, rasterExtent)(fillFn(pt))
    }
    vor.voronoiCellsWithPoints.foreach(rasterizeDistanceCell)

    tile
  }

}
