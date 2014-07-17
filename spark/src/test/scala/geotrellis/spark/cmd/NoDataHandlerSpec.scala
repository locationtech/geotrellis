package geotrellis.spark.cmd

import geotrellis.raster._
import geotrellis.raster.TypeBit
import geotrellis.raster.TypeByte
import geotrellis.raster.TypeDouble
import geotrellis.raster.TypeFloat
import geotrellis.raster.TypeInt
import geotrellis.raster.TypeShort
import geotrellis.raster.BitArrayTile
import geotrellis.raster.ByteArrayTile
import geotrellis.raster.DoubleArrayTile
import geotrellis.raster.FloatArrayTile
import geotrellis.raster.IntArrayTile
import geotrellis.raster.ShortArrayTile

import org.scalatest.FunSpec
import org.scalatest.matchers.ShouldMatchers

class NoDataHandlerSpec extends FunSpec with ShouldMatchers {
  describe("add/remove user nodata") {

    val cols = 2
    val rows = 1
    val size = cols * rows

    it("should correctly add/remove a user-defined nodata value to/from a FloatArrayTile") {
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
  }
}