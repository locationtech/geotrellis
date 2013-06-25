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

      val onesCoords = Set(   (1.0,0.0), (3.0,-0.0),
                            (1.0,-20.0),(3.0,-20.0),
                                                    (3.0,-20.0), (5.0,-20.0),
                                                    (3.0,-40.0), (5.0,-40.0))
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

    it("should vectorize an off shape.") {
      val n = NODATA
      val arr = 
        Array(
//             0  1  2  3  4  5  6  7  8  9  0  1
               n, n, n, n, n, n, n, n, n, n, n, n, // 0
               n, n, n, n, n, n, n, n, n, n, n, n, // 1
               n, n, n, n, n, n, n, n, n, n, n, n, // 2
               n, n, n, n, 1, 1, n, n, n, 1, 1, n, // 3
               n, n, n, 1, 1, 1, n, n, n, 1, 1, n, // 4
               n, n, 1, 1, 5, 1, n, n, 1, 1, n, n, // 5
               n, n, 1, 5, 5, 1, n, 1, 1, n, n, n, // 6 
               n, n, 5, 5, 1, 1, n, 1, n, n, n, n, // 7
               n, n, 5, 5, 1, 1, 1, 1, n, n, n, n, // 8
               n, n, n, n, n, n, n, n, n, n, n, n, // 9
               n, n, n, n, n, n, n, n, n, n, n, n, // 0
               n, n, n, n, n, n, n, n, n, n, n, n, // 1
               n, n, n, n, n, n, n, n, n, n, n, n, // 2
               n, n, n, n, n, n, n, n, n, n, n, n) // 3

      val cols = 12
      val rows = 14
      val cw = 1
      val ch = 10
      val xmin = 0
      val xmax = 12
      val ymin = -140
      val ymax = 0

      val r = Raster(arr,RasterExtent(Extent(xmin,ymin,xmax,ymax),cw,ch,cols,rows))


      val geoms = run(ToVector(r))

      def d(i:Int) = i.toDouble
      def tl(col:Int,row:Int) = (d(col),d(row)* -10)        // Top left
      def tr(col:Int,row:Int) = (d(col+1),(d(row))* -10)    // Top right
      def bl(col:Int,row:Int) = (d(col),d(row+1)* -10)        // Bottom left
      def br(col:Int,row:Int) = (d(col+1),(d(row)+1)* -10)    // Bottom right

      val onesCoords = Set(  tl(4,3), tr(5,3),                       tl(9,3), tr(10,3),
                    tl(3,4), tl(4,4),                                
                            br(3,4),  bl(5,4),              tl(8,5), tl(9,5),
            tl(2,5),tl(3,5),br(3,5),               tl(7,6), tl(8,6), br(9,4), br(10,4),
                    br(2,5),          br(5,7),     tl(7,8), br(8,5), br(9,5),
            bl(2,6),br(2,6),          bl(5,6),     br(7,6), br(8,6),
                               tl(4,7),
                               bl(4,8),            br(7,8),
         //To complete polygon:
              tl(2,5))


      geoms.length should be (2)

      val ones = geoms.filter(_.data == 1).map(_.asInstanceOf[Polygon[Int]]).toList
      ones.length should be (1)
      ones.map { polygon =>
        polygon.data should be (1)
        polygon.geom.getCoordinates.map(_.y).min should be (-90.0)
        polygon.geom.getCoordinates.map(_.y).max should be (-30.0)
        polygon.geom.getCoordinates.map(_.x).min should be (2.0)
        polygon.geom.getCoordinates.map(_.x).max should be (11.0)
        val coordinates = polygon.geom.getCoordinates.sortBy(_.x).map(c => (c.x,c.y))
        def coordString = {
          val sortedActual = coordinates.toSeq.sorted.toList
          val sortedExpected = onesCoords.toSeq.sorted.toList
          var s = "Values: (Actual,Expected)\n"
          for(x <- 0 until math.max(sortedActual.length,sortedExpected.length)) {
            if(x < sortedActual.length) { s += s" ${sortedActual(x)}  " }
            if(x < sortedExpected.length) { s += s" ${sortedExpected(x)}  \n" }
            else { s += "\n" }
          }
          s
        }
        withClue (s"$coordString\n\t\t") {
          coordinates.length should be (onesCoords.size + 1)
        }

        coordinates.map { c => 
          withClue (s"$c in expected coordinate set:") { onesCoords.contains(c) should be (true) }
        }
      }
    }
  }
}
