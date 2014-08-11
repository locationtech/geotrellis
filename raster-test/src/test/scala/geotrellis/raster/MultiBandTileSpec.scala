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

}
