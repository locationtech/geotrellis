package geotrellis.spark.cmd

import geotrellis._
import geotrellis.TypeBit
import geotrellis.TypeByte
import geotrellis.TypeDouble
import geotrellis.TypeFloat
import geotrellis.TypeInt
import geotrellis.TypeShort
import geotrellis.raster.BitArrayRasterData
import geotrellis.raster.ByteArrayRasterData
import geotrellis.raster.DoubleArrayRasterData
import geotrellis.raster.FloatArrayRasterData
import geotrellis.raster.IntArrayRasterData
import geotrellis.raster.ShortArrayRasterData

import org.scalatest.FunSpec
import org.scalatest.matchers.ShouldMatchers

class NoDataHandlerSpec extends FunSpec with ShouldMatchers {
  describe("add/remove user nodata") {

    val cols = 2
    val rows = 1
    val size = cols * rows

    it("should correctly add/remove a user-defined nodata value to/from a FloatArrayRasterData") {
      val userNoData = -9999.0f
      val origRd = FloatArrayRasterData(Array[Float](1.0f, Float.NaN), cols, rows)
      val addedRd = origRd.copy
      NoDataHandler.addUserNoData(addedRd, userNoData)

      {
        val actualUndAdded = addedRd.asInstanceOf[FloatArrayRasterData].array
        val expectedUndAdded = FloatArrayRasterData(Array[Float](1.0f, userNoData), cols, rows).array
        expectedUndAdded should be(actualUndAdded)
      }

      val removedRd = addedRd.copy
      NoDataHandler.removeUserNoData(removedRd, userNoData)
      
      {
        val actualUndRemoved = removedRd.asInstanceOf[FloatArrayRasterData].array
        val expectedUndRemoved = origRd.asInstanceOf[FloatArrayRasterData].array
        expectedUndRemoved(0) should be(actualUndRemoved(0))
        expectedUndRemoved(1).isNaN should be(true)
      }
    }
    
    it("should correctly add/remove a user-defined nodata value to/from a IntArrayRasterData") {
      // user's nodata is Int.MaxVal whereas NODATA = Int.MinVal
      val userNoData = Int.MaxValue
      val origRd = IntArrayRasterData(Array[Int](1, NODATA), cols, rows)
      val addedRd = origRd.copy
      NoDataHandler.addUserNoData(addedRd, userNoData)

      {
        val actualUndAdded = addedRd.asInstanceOf[IntArrayRasterData].array
        val expectedUndAdded = IntArrayRasterData(Array[Int](1, userNoData), cols, rows).array
        expectedUndAdded should be(actualUndAdded)
      }

      val removedRd = addedRd.copy
      NoDataHandler.removeUserNoData(removedRd, userNoData)
      
      {
        val actualUndRemoved = removedRd.asInstanceOf[IntArrayRasterData].array
        val expectedUndRemoved = origRd.asInstanceOf[IntArrayRasterData].array
        expectedUndRemoved should be(actualUndRemoved)
      }
    }
  }
}