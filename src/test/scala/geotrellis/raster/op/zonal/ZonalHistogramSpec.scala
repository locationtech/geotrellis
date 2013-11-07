package geotrellis.raster.op.zonal

import geotrellis._

import org.scalatest.FunSpec
import org.scalatest.matchers.ShouldMatchers

import geotrellis.testutil._

import scala.collection.mutable

class ZonalHistogramSpec extends FunSpec 
                             with ShouldMatchers 
                             with TestServer 
                             with RasterBuilders {
  describe("ZonalHistogram") {
    it("gives correct histogram map for example raster") { 
      val r = createRaster(
        Array(1, 2, 2, 2, 3, 1, 6, 5, 1,
              1, 2, 2, 2, 3, 6, 6, 5, 5,
              1, 3, 5, 3, 6, 6, 6, 5, 5,
              3, 1, 5, 6, 6, 6, 6, 6, 2,
              7, 7, 5, 6, 1, 3, 3, 3, 2,
              7, 7, 5, 5, 5, 4, 3, 4, 2,
              7, 7, 5, 5, 5, 4, 3, 4, 2,
              7, 2, 2, 5, 4, 4, 3, 4, 4),
        9,8)

      // 1 -
      // 2 - 15
      // 3 - 12
      // 4 - 6
      // 5 - 6
      // 6 - 6
      // 7 - 12
      // 8 - 6
      val zones = createRaster(
        Array(1, 1, 1, 4, 4, 4, 5, 6, 6,
              1, 1, 1, 4, 4, 5, 5, 6, 6,
              1, 1, 2, 4, 5, 5, 5, 6, 6,
              1, 2, 2, 3, 3, 3, 3, 3, 3,
              2, 2, 2, 3, 3, 3, 3, 3, 3,
              2, 2, 2, 7, 7, 7, 7, 8, 8,
              2, 2, 2, 7, 7, 7, 7, 8, 8,
              2, 2, 2, 7, 7, 7, 7, 8, 8),
        9,8)

      val (cols,rows) = (zones.cols,zones.rows)

      val zoneValues = mutable.Map[Int,mutable.ListBuffer[Int]]()

      for(row <- 0 until rows) {
        for(col <- 0 until cols) {
          val z = zones.get(col,row)
          if(!zoneValues.contains(z)) { zoneValues(z) = mutable.ListBuffer[Int]() }
          zoneValues(z) += r.get(col,row)
        }
      }

      val expected = 
        zoneValues.toMap.mapValues { list =>
          list.distinct
              .map { v => (v, list.filter(_ == v).length) }
              .toMap
        }

      val result = run(ZonalHistogram(r,zones))

      result.keys should be (expected.keys)

      for(zone <- result.keys) {
        val hist = result(zone)
        for(v <- expected(zone).keys) {
          hist.getItemCount(v) should be (expected(zone)(v))
        }
      }
    }

    it("gives correct histogram map for example raster sources") {
      val rs = createRasterSource(
        Array(1, 2, 2,   2, 3, 1,   6, 5, 1,
              1, 2, 2,   2, 3, 6,   6, 5, 5,

              1, 3, 5,   3, 6, 6,   6, 5, 5,
              3, 1, 5,   6, 6, 6,   6, 6, 2,

              7, 7, 5,   6, 1, 3,   3, 3, 2,
              7, 7, 5,   5, 5, 4,   3, 4, 2,

              7, 7, 5,   5, 5, 4,   3, 4, 2,
              7, 2, 2,   5, 4, 4,   3, 4, 4),
        3,4,3,2)

      val zonesSource = createRasterSource(
        Array(1, 1, 1,   4, 4, 4,   5, 6, 6,
              1, 1, 1,   4, 4, 5,   5, 6, 6,

              1, 1, 2,   4, 5, 5,   5, 6, 6,
              1, 2, 2,   3, 3, 3,   3, 3, 3,

              2, 2, 2,   3, 3, 3,   3, 3, 3,
              2, 2, 2,   7, 7, 7,   7, 8, 8,

              2, 2, 2,   7, 7, 7,   7, 8, 8,
              2, 2, 2,   7, 7, 7,   7, 8, 8),
        3,4,3,2)

      val r = runSource(rs)
      val zones = runSource(zonesSource)

      val (cols,rows) = (zones.cols,zones.rows)

      val zoneValues = mutable.Map[Int,mutable.ListBuffer[Int]]()

      for(row <- 0 until rows) {
        for(col <- 0 until cols) {
          val z = zones.get(col,row)
          if(!zoneValues.contains(z)) { zoneValues(z) = mutable.ListBuffer[Int]() }
          zoneValues(z) += r.get(col,row)
        }
      }

      val expected = 
        zoneValues.toMap.mapValues { list =>
          list.distinct
              .map { v => (v, list.filter(_ == v).length) }
              .toMap
        }

      val result = runSource(rs.zonalHistogram(zonesSource))

      result.keys should be (expected.keys)

      for(zone <- result.keys) {
        val hist = result(zone)
        for(v <- expected(zone).keys) {
          hist.getItemCount(v) should be (expected(zone)(v))
        }
      }
    }

    // it("gives correct percentage for example raster sources") { 
    //   val rs = createRasterSource(
    //     Array(1, 2, 2,   2, 3, 1,   6, 5, 1,
    //           1, 2, 2,   2, 3, 6,   6, 5, 5,

    //           1, 3, 5,   3, 6, 6,   6, 5, 5,
    //           3, 1, 5,   6, 6, 6,   6, 6, 2,

    //           7, 7, 5,   6, 1, 3,   3, 3, 2,
    //           7, 7, 5,   5, 5, 4,   3, 4, 2,

    //           7, 7, 5,   5, 5, 4,   3, 4, 2,
    //           7, 2, 2,   5, 4, 4,   3, 4, 4),
    //     3,4,3,2)

    //   // 1 - 9
    //   // 2 - 15
    //   // 3 - 12
    //   // 4 - 6
    //   // 5 - 6
    //   // 6 - 6
    //   // 7 - 12
    //   // 8 - 6
    //   val zonesSource = createRasterSource(
    //     Array(1, 1, 1,   4, 4, 4,   5, 6, 6,
    //           1, 1, 1,   4, 4, 5,   5, 6, 6,

    //           1, 1, 2,   4, 5, 5,   5, 6, 6,
    //           1, 2, 2,   3, 3, 3,   3, 3, 3,

    //           2, 2, 2,   3, 3, 3,   3, 3, 3,
    //           2, 2, 2,   7, 7, 7,   7, 8, 8,

    //           2, 2, 2,   7, 7, 7,   7, 8, 8,
    //           2, 2, 2,   7, 7, 7,   7, 8, 8),
    //     3,4,3,2)

    //   val expected = Map(
    //     1 -> Map(
    //       1 -> 33,
    //       2 -> 44,
    //       3 -> 22
    //     ),
    //     2 -> Map(
    //       7 -> 47,
    //       2 -> 13,
    //       5 -> 33,
    //       1 ->  7
    //     ),
    //     3 -> Map(
    //       1 -> 8,
    //       6 -> 50,
    //       3 -> 25,
    //       2 -> 17
    //     ),
    //     4 -> Map(
    //       1 -> 17,
    //       2 -> 33,
    //       3 -> 50
    //     ),
    //     5 -> Map(
    //       6 -> 100
    //     ),
    //     6 -> Map(
    //       1 -> 17,
    //       5 -> 83
    //     ),
    //     7 -> Map(
    //       3 -> 25,
    //       4 -> 33,
    //       5 -> 42
    //     ),
    //     8 -> Map(
    //       2 -> 33,
    //       4 -> 67
    //     )
    //   )

    //   val result = runSource(rs.zonalHistogram(zonesSource))
    //   val r = runSource(rs)
    //   val zones = runSource(zonesSource)
    //   val (cols,rows) = (r.rasterExtent.cols, r.rasterExtent.rows)
    //   for(col <- 0 until cols) {
    //     for(row <- 0 until rows) {
    //       val zone = zones.get(col,row)
    //       val value = r.get(col,row)
    //       val percentage = result.get(col,row)
    //       withClue(s"Expected($zone)($value) = ${expected(zone)(value)}, Actual = $percentage") {
    //         percentage should be (expected(zone)(value))
    //       }
    //     }
    //   }
    // }
  }
}
