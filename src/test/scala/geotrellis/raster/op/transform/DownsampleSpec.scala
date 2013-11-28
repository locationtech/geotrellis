package geotrellis.raster.op.transform

import geotrellis._

import geotrellis.testutil._

import org.scalatest.FunSpec
import org.scalatest.matchers.ShouldMatchers

import scala.collection.mutable

class DownsampleSpec extends FunSpec with ShouldMatchers 
                               with TestServer
                               with RasterBuilders {
  describe("Downsample") {
    it("downsamples with mode") {
      val r = createRaster(Array(1,2,1,1, 2,1,2,2, 3,2,3,3, 4,3,4,4,
                                 1,2,2,1, 2,1,1,2, 3,2,2,3, 4,3,3,4,
                                 1,1,2,1, 2,2,1,2, 3,3,2,3, 4,4,3,4,

                                 4,1,4,4, 3,1,3,3, 2,1,2,2, 1,2,1,1,
                                 4,1,1,4, 3,1,1,3, 2,1,1,2, 1,2,2,1,
                                 4,4,1,4, 3,3,1,3, 2,2,1,2, 1,1,2,1,

                                 2,1,2,2, 3,1,3,3, 4,2,4,4, 1,2,1,1,
                                 2,1,1,2, 3,1,1,3, 4,2,2,4, 1,2,2,1,
                                 2,2,1,2, 3,3,1,3, 4,4,2,4, 1,1,2,1), 16, 9)

      val op = Downsample(r,4,3)({
        cellSet =>
          var counts = mutable.Map((1,0),(2,0),(3,0),(4,0))
          cellSet.foreach({ (col,row) => counts(r.get(col,row)) = counts(r.get(col,row)) + 1 })
          var maxValue = 0
          var maxCount = 0
          for( (value,count) <- counts) {
            if(count > maxCount) {
              maxCount = count
              maxValue = value
            }
          }
          maxValue
      })

      val result = get(op)
      result.cols should be (4)
      result.rows should be (3)
      assertEqual(result, Array( 1, 2, 3, 4,
                                 4, 3, 2, 1,
                                 2, 3, 4, 1))
    }

    it("downsamples with max") {
      val r = createRaster(Array(1,2,1,1, 2,1,2,2, 3,2,3,3, 4,3,4,4,
                                 1,2,2,1, 2,1,1,2, 3,2,2,3, 4,3,3,4,
                                 1,1,2,1, 2,2,1,2, 3,3,2,3, 4,4,3,4,

                                 4,1,4,4, 3,1,3,3, 2,1,2,2, 1,2,1,1,
                                 4,1,1,4, 3,1,1,3, 2,1,1,2, 1,2,2,1,
                                 4,4,1,4, 3,3,1,3, 2,2,1,2, 1,1,2,1,

                                 2,1,2,2, 3,1,3,3, 4,2,4,4, 1,2,1,1,
                                 2,1,1,2, 3,1,1,3, 4,2,2,4, 1,2,2,1,
                                 2,2,1,2, 3,3,1,3, 4,4,2,4, 1,1,2,1), 16, 9)

      val op = Downsample(r,4,3)({
        cellSet =>
          var maxValue = 0
          cellSet.foreach({ (col,row) => if(r.get(col,row) > maxValue) maxValue = r.get(col,row) })
          maxValue
      })

      val result = get(op)
      result.cols should be (4)
      result.rows should be (3)
      assertEqual(result, Array( 2, 2, 3, 4,
                                 4, 3, 2, 2,
                                 2, 3, 4, 2))
    }

    it("downsamples with max, when the cols don't divide evenly") {
      val r = createRaster(Array(1,2,1,1,2, 1,2,2,3,2, 3,3,4,3,4, 4,2,3,
                                 1,2,2,1,2, 1,1,2,3,2, 2,3,4,3,3, 4,2,3,
                                 1,1,2,1,2, 2,1,2,3,3, 2,3,4,4,3, 4,2,5,

                                 4,1,4,4,3, 1,3,3,2,1, 2,2,1,2,1, 1,1,6,
                                 4,1,1,4,3, 1,1,3,2,1, 1,2,1,2,2, 1,2,1,
                                 4,4,1,4,3, 3,1,3,2,2, 1,2,1,1,2, 1,4,3,

                                 2,1,2,2,3, 1,3,3,4,2, 4,4,1,2,1, 1,2,1,
                                 2,1,1,2,3, 1,1,3,4,2, 2,4,1,2,2, 1,4,8,
                                 2,2,1,2,3, 3,1,3,4,4, 2,4,1,1,2, 1,2,6), 18, 9)

      val op = Downsample(r,4,3)({
        cellSet =>
          var maxValue = 0
          cellSet.foreach({ 
            (col,row) => 
              if(col < r.cols && row < r.rows) {
                if(r.get(col,row) > maxValue) maxValue = r.get(col,row) 
              }
          })
          maxValue
      })

      val result = get(op)
      
      result.cols should be (4)
      result.rows should be (3)

      result.rasterExtent.extent.xmax should be (r.rasterExtent.extent.xmax + 2*r.rasterExtent.cellwidth)
      result.rasterExtent.extent.ymax should be (r.rasterExtent.extent.ymax)
      assertEqual(result, Array( 2, 3, 4, 5,
                                 4, 3, 2, 6, 
                                 3, 4, 4, 8))
    }

    it("downsamples with max, when the rows don't divide evenly") {
      val r = createRaster(Array(1,2,1,1, 2,1,2,2, 3,2,3,3, 4,3,4,4,
                                 1,2,2,1, 2,1,1,2, 3,2,2,3, 4,3,3,4,
                                 1,1,2,1, 2,2,1,2, 3,3,2,3, 4,4,3,4,

                                 4,1,4,4, 3,1,3,3, 2,1,2,2, 1,2,1,1,
                                 4,1,1,4, 3,1,1,3, 2,1,1,2, 1,2,2,1,
                                 4,4,1,4, 3,3,1,3, 2,2,1,2, 1,1,2,1,

                                 2,1,2,2, 3,1,3,3, 4,2,4,4, 1,2,1,1,
                                 2,1,1,2, 3,1,1,3, 4,2,2,4, 1,2,2,1), 16, 8)

      val op = Downsample(r,4,3)({
        cellSet =>
          var maxValue = 0
          cellSet.foreach({ 
            (col,row) => 
              if(col < r.cols && row < r.rows) {
                if(r.get(col,row) > maxValue) maxValue = r.get(col,row) 
              }
          })
          maxValue
      })

      val result = get(op)
      
      result.cols should be (4)
      result.rows should be (3)

      result.rasterExtent.extent.xmax should be (r.rasterExtent.extent.xmax)
      result.rasterExtent.extent.ymax should be (r.rasterExtent.extent.ymax + r.rasterExtent.cellheight)

      assertEqual(result, Array( 2, 2, 3, 4,
                                 4, 3, 2, 2,
                                 2, 3, 4, 2))
    }
  }
}
