package geotrellis.raster.op

import geotrellis._
import geotrellis.testutil._
import geotrellis.feature._

import spire.syntax._
import scala.collection.mutable

import org.scalatest.FunSpec

class ToVectorSpec extends FunSpec
                      with TestServer
                      with RasterBuilders {
  describe("ToVector") {
    it("should get correct vectors.") {
      val n = NODATA
      val arr = 
        Array( n, 1, 1, n, n, n,
               n, 1, 1, n, n, n,
               n, n, n, 1, 1, n,
               n, n, n, 1, 1, n,
               n, n, n, n, n, n)

      val cols = 6
      val rows = 5
      val cw = 1
      val ch = 10
      val xmin = 0
      val xmax = 6
      val ymin = -50
      val ymax = 0

      val r = Raster(arr,RasterExtent(Extent(xmin,ymin,xmax,ymax),cw,ch,cols,rows))

      val geoms = run(ToVector(r))

      val onesCoords = Set( (1.5,-5.0),  (2.5,-5.0),
                           (1.5,-15.0),(2.5,-15.0),
                                                    (3.5,-25.0), (4.5,-25.0),
                                                    (3.5,-35.0), (4.5,-35.0))
      val ones = geoms.filter(_.data == 1).map(_.asInstanceOf[Polygon[Int]]).toList
      ones.length should be (2)
      ones.map { polygon =>
        polygon.data should be (1)
        val coordinates = polygon.geom.getCoordinates.sortBy(_.x).map(c => (c.x,c.y))
        coordinates.length should be (5)

        coordinates.map { c => 
          withClue (s"$c in expected coordinate set:") { onesCoords.contains(c) should be (true) }
        }
      }
    }
  }
}
