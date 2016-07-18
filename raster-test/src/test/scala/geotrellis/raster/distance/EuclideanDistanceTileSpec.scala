package geotrellis.raster.distance

import geotrellis.raster._
import geotrellis.raster.testkit._
import geotrellis.vector._
import scala.math.{sqrt, pow}

import org.scalatest._

class EuclideanDistanceTileSpec extends FunSpec with Matchers with RasterMatchers {
  describe("Euclidean Distance Tiles") {
    it("should produce the same result as using explicit nearest neighbor search") {
      val pts = Array(Point(0,0), Point(3,1), Point(7,5), Point(4,7), Point(8,3), Point(1,3))
      val rasterExtent = RasterExtent(Extent(0, 0, 8, 8), 512, 512)

      val euclideanDistanceTile = pts.euclideanDistanceTile(rasterExtent)

      def pointToTup(pt: Point): (Double, Double) = (pt.x, pt.y)
      val index = SpatialIndex(pts)(pointToTup(_))
      def smallestDistanceToPoint(tup: (Double, Double)): Double = {
        val nearest = index.nearest(tup)
        sqrt(pow(tup._1 - nearest.x, 2) + pow(tup._2 - nearest.y, 2))
      }

      val tile = DoubleArrayTile.empty(512, 512)
      for (col <- 0 until 512;
           row <- 0 until 512)
        tile.setDouble(col, row, smallestDistanceToPoint(rasterExtent.gridToMap(col, row)))

      assertEqual(tile, euclideanDistanceTile)
    }
  }
}
