package geotrellis.raster

import geotrellis.vector.Extent
import geotrellis.testkit._
import geotrellis.raster.op.local._
import org.scalatest._
import scala.collection.mutable

class MultiBandTileSpec extends FunSpec
  with Matchers
  with TestEngine {

  val array1 = Array(0, -1, 2, -3,
    4, -5, 6, -7,
    8, -9, 10, -11,
    12, -13, 14, -15)

  val array2 = Array(0, 4, -5, 6,
    -1, 2, -3, -7,
    12, -13, 14, -15,
    8, -9, 10, -11)

  val array3 = Array(0, 0, 2, -3,
    4, -5, 6, -7,
    8, 0, 10, 0,
    12, -13, 14, -15)

  val array4 = Array(10, -1, 2, -3,
    4, -5, 6, -7,
    8, -9, 10, -11,
    12, -13, 14, -15)

  val tile1 = IntArrayTile(array1, 4, 4)
  val tile2 = IntArrayTile(array2, 4, 4)
  val tile3 = IntArrayTile(array3, 4, 4)
  val tile4 = IntArrayTile(array4, 4, 4)

  val bArray1: Array[Byte] = Array(0, -1, 2, -3,
    4, -5, 6, -7,
    8, -9, 10, -11,
    12, -13, 14, -15)

  val bArray2: Array[Byte] = Array(0, 4, -5, 6,
    -1, 2, -3, -7,
    12, -13, 14, -15,
    8, -9, 10, -11)

  val bArray3: Array[Byte] = Array(0, 0, 2, -3,
    4, -5, 6, -7,
    8, 0, 10, 0,
    12, -13, 14, -15)

  val bArray4: Array[Byte] = Array(10, -1, 2, -3,
    4, -5, 6, -7,
    8, -9, 10, -11,
    12, -13, 14, -15)

  val bTile1 = ByteArrayTile(bArray1, 4, 4)
  val bTile2 = ByteArrayTile(bArray2, 4, 4)
  val bTile3 = ByteArrayTile(bArray3, 4, 4)
  val bTile4 = ByteArrayTile(bArray4, 4, 4)

  describe("A MultiBandTile") {

    val multiBand = MultiBandTile(Array(tile1, tile2, tile3, tile4))

    it("should get band at any index") {
      multiBand.getBand(2) should be(tile3)
    }

    it("should match dimensions") {
      multiBand.dimensions should be((4, 4))
    }

    it("should be comparable to others") {
      val multiBand1 = MultiBandTile(Array(tile3, tile2, tile4, tile1))
      val multiBand2 = MultiBandTile(Array(tile4, tile3, tile2, tile1))
      val multiBand3 = MultiBandTile(Array(tile1, tile2, tile3))
      val multiBand4 = MultiBandTile(Array(tile1, tile2, tile3, tile4))

      multiBand should not be (multiBand1)
      multiBand should not be (multiBand2)
      multiBand should not be (multiBand3)
      multiBand should be(multiBand4)
    }

    it("should create empty MultiBandTiles") {
      val mt = MultiBandTile.empty(TypeInt, 10, 10, 10)
      for (i <- 0 until 10) {
        var result = mt.getBand(i)
        for (c <- 0 until result.cols) {
          for (r <- 0 until result.rows) {
            result.get(c, r) should be(NODATA)
          }
        }
      }
    }
  }

  describe("convert") {
    it("should convert a Byte MultiBandTile to an Int MultiBandTile and vice versa") {
      val byteMultiBand = MultiBandTile(Array(bTile1, bTile2, bTile3, bTile4))
      val intMultiBand = MultiBandTile(Array(tile1, tile2, tile3, tile4))

      /**
       * convert Byte MultiBandTile to Int MultiBandTile
       */
      for (i <- 0 until byteMultiBand.bands) {
        var result = (byteMultiBand convert (TypeInt)).getBand(i).localAdd(10)
        var comp = intMultiBand.getBand(i)
        result.cellType should be(comp.cellType)
        for (c <- 0 until result.cols) {
          for (r <- 0 until result.rows) {
            result.get(c, r) should be(comp.get(c, r) + 10)
          }
        }
      }

      /**
       * convert Int MultiBandTile to Byte MultiBandTile
       */
      for (i <- 0 until intMultiBand.bands) {
        var result = (intMultiBand convert (TypeByte)).getBand(i).localAdd(10)
        var comp = byteMultiBand.getBand(i)
        result.cellType should be(comp.cellType)
        for (c <- 0 until result.cols) {
          for (r <- 0 until result.rows) {
            result.get(c, r) should be(comp.get(c, r) + 10)
          }
        }
      }
    }
  }

  describe("mapIfSet") {
    val arr1 = Array(NODATA, -1, 2, -3,
      4, -5, 6, NODATA,
      8, NODATA, 10, -11,
      NODATA, -13, 14, -15)

    val arr2 = Array(NODATA, 4, -5, 6,
      -1, NODATA, -3, -7,
      NODATA, -13, 14, -15,
      8, -9, 10, NODATA)

    val arr3 = Array(NODATA, NODATA, 2, -3,
      4, -5, 6, -7,
      8, NODATA, 10, NODATA,
      12, -13, NODATA, -15)

    val n = Float.NaN
    val arr4 = Array(n, -1.0f, 2.0f, -3.0f,
      4.0f, -5.0f, 6.0f, n,
      8.0f, n, 10.0f, -11.0f,
      n, -13.0f, 14.0f, -15.0f)

    val arr5 = Array(n, 4.0f, -5.0f, 6.0f,
      -1.0f, n, -3.0f, -7.0f,
      n, -13.0f, 14.0f, -15.0f,
      8.0f, -9.0f, 10.0f, n)

    val arr6 = Array(n, n, 2.0f, -3.0f,
      4.0f, -5.0f, 6.0f, -7.0f,
      8.0f, n, 10.0f, n,
      12.0f, -13.0f, n, -15.0f)

    def check(m: MultiBandTile) {
      val m1 = m.mapIfSet(a => a * a)
      for (i <- 0 until m.bands) {
        for (c <- 0 until m.cols) {
          for (r <- 0 until m.rows) {
            if (isNoData(m.getBand(i).get(c, r)))
              isNoData(m.getBand(i).get(c, r)) should be(true)
          }
        }
      }
    }

    def checkDouble(m: MultiBandTile) {
      val m1 = m.mapIfSetDouble(a => 0.1991)
      for (i <- 0 until m.bands) {
        for (c <- 0 until m.cols) {
          for (r <- 0 until m.rows) {
            if (isNoData(m.getBand(i).getDouble(c, r)))
              isNoData(m.getBand(i).getDouble(c, r)) should be(true)
          }
        }
      }
    }

    it("should respect NoData values in Int MultiBandTile") {
      val t1 = IntArrayTile(arr1, 4, 4)
      val t2 = IntArrayTile(arr2, 4, 4)
      val t3 = IntArrayTile(arr3, 4, 4)
      val mb = MultiBandTile(Array(t1, t2, t3))

      withClue("Int MultiBandTile") { check(mb) }
    }

    it("should respect NoData values in Float MultiBandTile") {
      val t4 = FloatArrayTile(arr4, 4, 4)
      val t5 = FloatArrayTile(arr5, 4, 4)
      val t6 = FloatArrayTile(arr6, 4, 4)
      val mb1 = MultiBandTile(Array(t4, t5, t6))

      withClue("Float MultiBandTile") { checkDouble(mb1) }
    }
  }

  describe("warp") {
    val ext = Extent(0.0, 0.0, 4.0, 4.0)
    val nre = RasterExtent(Extent(0.0, 2.0, 3.0, 4.0), 3, 2)
    val multiBand = MultiBandTile(Array(tile1, tile2, tile3, tile4))

    val narray1 = Array(0, -1, 2,
      4, -5, 6)

    val narray2 = Array(0, 4, -5,
      -1, 2, -3)

    val narray3 = Array(0, 0, 2,
      4, -5, 6)

    val narray4 = Array(10, -1, 2,
      4, -5, 6)
    val m = multiBand.warp(ext, nre)

    it("should warp to target dimensions") {
      val targetCols = 2
      val targetRows = 3
      val result = multiBand.warp(ext, targetCols, targetRows)
      result.cols should be(2)
      result.rows should be(3)
    }

    it("should warp with crop only") {
      m.getBand(0).toArray should be(narray1)
      m.getBand(1).toArray should be(narray2)
      m.getBand(2).toArray should be(narray3)
      m.getBand(3).toArray should be(narray4)
    }
  }

}
