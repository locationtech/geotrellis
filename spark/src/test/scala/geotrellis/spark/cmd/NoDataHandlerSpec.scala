package geotrellis.spark.cmd

import geotrellis._
import geotrellis.raster._
import org.scalatest._

class NoDataHandlerSpec extends FunSpec with Matchers {
  describe("add/remove user nodata") {

    val cols = 2
    val rows = 1
    val size = cols * rows

    it("should correctly add/remove a user-defined nodata value to/from a FloatArrayTile") {
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
    
    it("should correctly add/remove a user-defined nodata value to/from a IntArrayTile") {
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