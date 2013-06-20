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

    // it("should get correct vectors.") {
    //   val n = NODATA
    //   val arr = 
    //     Array( n, 1, 1, n, n, n, n, n, n, n, 7, 7, n, n, n, n, n,
    //            n, 1, 1, n, n, n, n, n, n, n, 7, 7, n, n, n, n, n,
    //            n, n, n, 1, 1, n, n, n, n, n, 7, n, n, 7, n, n, n,
    //            n, n, n, 1, 1, n, n, n, n, n, 7, 7, n, 7, 7, n, n,
    //            n, n, n, n, n, n, n, n, n, n, n, 7, n, n, 7, n, n,
    //            n, n, n, n, n, n, n, n, n, n, n, 7, 7, 7, 7, n, n,
    //            n, n, n, n, n, n, n, n, n, n, n, n, n, 7, n, n, n,
    //            n, n, n, n, n, n, n, n, n, n, n, n, n, n, n, n, n,
    //            n, n, n, n, 3, n, n, 3, n, n, n, n, n, n, n, n, n,
    //            n, n, 3, 3, 3, 3, 3, 3, 3, 3, n, n, n, n, n, n, n,
    //            n, n, n, n, n, n, 5, n, n, 3, 3, n, n, n, n, 8, 8,
    //            n, n, n, n, n, n, 5, n, n, n, n, n, n, n, n, 8, n,
    //            n, n, n, n, n, n, 5, n, n, n, n, n, n, 8, 8, 8, n,
    //            n, n, n, n, n, n, 5, 5, n, n, n, n, n, 8, n, n, n,
    //            n, n, n, 5, 5, n, 5, n, n, n, n, n, n, n, n, n, n,
    //            n, n, 5, 5, 5, n, 5, n, n, n, n, n, n, n, n, n, n,
    //            n, 5, 5, 5, 5, 5, 5, n, n, 5, 5, n, n, n, n, n, n,
    //            n, n, n, n, n, n, n, n, n, n, n, 5, 5, n, n, n, n)

    //   val cols = 17
    //   val rows = 18
    //   val cw = 1
    //   val ch = 10
    //   val xmin = 0
    //   val xmax = 17
    //   val ymin = -180
    //   val ymax = 0

    //   val r = Raster(arr,RasterExtent(Extent(xmin,ymin,xmax,ymax),cw,ch,cols,rows))

    //   val geoms = run(ToVector(r))

    //   val ones = geoms.filter(_.data == 1).map(_.asInstanceOf[Polygon[Int]]).toList
    //   ones.length should be (2)
    //   ones.map { polygon =>
    //     val coordinates = polygon.geom.getCoordinates.sortBy(_.x)
    //     println(coordinates.map { c => s"Coordinate(${c.x},${c.y})" }.mkString(","))
    //     coordinates.length should be (4)
    //     if(coordinates(0).x == 1.5) {

    //     } else {
    //       if(coordinates(0).x == 3.5) {

    //       } else {
    //         assert(false,s"${coordinates(0).x} is not 1.5 or 3.5")
    //       }
    //     }
    //   }
    // }

    // it("should group regions when regions are concentric.") {
    //   val n = NODATA
    //   val r = createRaster(
    //     Array(
    //            n, n, 7, 7, 7, n, n,
    //            n, 7, 7, 5, 7, 7, n,
    //            7, 7, 5, 5, 5, 7, 7,
    //            7, 5, 5, 9, 5, 5, 7,
    //            7, 5, 5, 7, 5, 7, 7,
    //            7, 7, 5, 5, 5, 7, n,
    //            n, 7, 5, 5, 7, 7, n,
    //            n, 7, 7, 7, 7, n, n
    //     ),
    //            7,8
    //   )

    //   val RegionGroupResult(regions,regionMap) = run(RegionGroup(r))

    //   val histogram = run(GetHistogram(regions))
    //   val count = histogram.getValues.length
    //   count should be (4)
      
    //   val regionCounts = mutable.Map[Int,mutable.Set[Int]]()
    //   cfor(0)(_ < 7, _ + 1) { col =>
    //     cfor(0)(_ < 8, _ + 1) { row =>
    //       val v = r.get(col,row)
    //       val region = regions.get(col,row)
    //       if(v == NODATA) { region should be (v) }
    //       else {
    //         regionMap(region) should be (v)
    //         if(!regionCounts.contains(v)) { regionCounts(v) = mutable.Set[Int]() }
    //         regionCounts(v) += region
    //       }
    //     }
    //   }

    //   regionCounts(7).size should be (2)
    //   regionCounts(9).size should be (1)
    //   regionCounts(5).size should be (1)
    // }
  }
}
