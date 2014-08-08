package geotrellis.spark.cmd

import geotrellis.raster._

import geotrellis.testkit._

import org.scalatest._

class NoDataHandlerSpec extends FunSpec with Matchers with TestEngine {
  describe("add/remove user nodata") {
    it("should correctly add/remove a user-defined nodata value to/from a FloatArrayTile") {
      val cols = 2
      val rows = 1
      val size = cols * rows

      val userNoData = -9999.0f
      val origRd = FloatArrayTile(Array[Float](1.0f, Float.NaN), cols, rows)
      val addedRd = origRd.copy()
      NoDataHandler.addUserNoData(addedRd, userNoData)

      {
        val actualUndAdded = addedRd.asInstanceOf[FloatArrayTile].array
        val expectedUndAdded = FloatArrayTile(Array[Float](1.0f, userNoData), cols, rows).array
        expectedUndAdded should be(actualUndAdded)
      }

      val removedRd = addedRd.copy()
      NoDataHandler.removeUserNoData(removedRd, userNoData)
      
      {
        val actualUndRemoved = removedRd.asInstanceOf[FloatArrayTile].array
        val expectedUndRemoved = origRd.asInstanceOf[FloatArrayTile].array
        expectedUndRemoved(0) should be(actualUndRemoved(0))
        expectedUndRemoved(1).isNaN should be(true)
      }
    }
    
    it("should correctly add/remove a user-defined nodata value to/from a IntArrayTile") {
      // user's nodata is Int.MaxVal whereas NODATA = Int.MinVal
      val cols = 2
      val rows = 1
      val size = cols * rows

      val userNoData = Int.MaxValue
      val origRd = IntArrayTile(Array[Int](1, NODATA), cols, rows)
      val addedRd = origRd.copy()
      NoDataHandler.addUserNoData(addedRd, userNoData)

      {
        val actualUndAdded = addedRd.asInstanceOf[IntArrayTile].array
        val expectedUndAdded = IntArrayTile(Array[Int](1, userNoData), cols, rows).array
        expectedUndAdded should be(actualUndAdded)
      }

      val removedRd = addedRd.copy()
      NoDataHandler.removeUserNoData(removedRd, userNoData)
      
      {
        val actualUndRemoved = removedRd.asInstanceOf[IntArrayTile].array
        val expectedUndRemoved = origRd.asInstanceOf[IntArrayTile].array
        expectedUndRemoved should be(actualUndRemoved)
      }
    }

    it("should correctly add/remove a user-defined nodata value to/from a ByteArrayTile") {
      // user's nodata is Int.MaxVal whereas NODATA = Int.MinVal
      val cols = 256
      val rows = 256
      val size = cols * rows

      val userNoData = Byte.MaxValue
      val gtArr = Array.ofDim[Byte](cols*rows).fill(byteNODATA)
      val userArr = Array.ofDim[Byte](cols*rows).fill(userNoData)
      val gtTile = ByteArrayTile(gtArr, cols, rows)
      val userTile = ByteArrayTile(userArr, cols, rows)

      val addedTile = gtTile.copy()
      NoDataHandler.addUserNoData(addedTile, userNoData)

      assertEqual(addedTile, userTile)

      val removedTile = addedTile.copy()
      NoDataHandler.removeUserNoData(removedTile, userNoData)

      assertEqual(removedTile, gtTile)
    }
  }
}
