package geotrellis.raster.op.hydrology

import geotrellis._
import geotrellis.source._
import geotrellis.raster._

import org.scalatest.FunSpec
import org.scalatest.matchers.ShouldMatchers

import geotrellis.testutil._

class FlowDirectionSpec extends FunSpec with ShouldMatchers 
                                        with TestServer 
                                        with RasterBuilders {
  describe("FlowDirection") {

    // Note: Sinks are not autmatically filled, so sinks are always going to have NODATA for direction
    it("should match computed elevation raster") {
      val ncols = 6
      val nrows = 6
      val r_extent = RasterExtent(Extent(0,0,1,1),1,1,ncols,nrows)
      val e = IntArrayRasterData(Array[Int](78,72,69,71,58,49,
                                            74,67,56,49,46,50,
                                            69,53,44,37,38,48,
                                            64,58,55,22,31,24,
                                            68,61,47,21,16,19,
                                            74,53,34,12,11,12),
                            ncols,nrows)
      val e_raster = Raster(e, r_extent)
      val m = IntArrayRasterData(Array[Int](2,2,2,4,4,8,
                                            2,2,2,4,4,8,
                                            1,1,2,4,8,4,
                                            128,128,1,2,4,8,
                                            2,2,1,4,4,4,
                                            1,1,1,1,NODATA,16),
                            ncols,nrows)
      val m_raster = Raster(m, r_extent)
      val e_computed = FlowDirection(e_raster)
      assertEqual(e_computed, m_raster)
    }

    it("should have NODATA direction for sinks") {
      val ncols = 3
      val nrows = 3
      val r_extent = RasterExtent(Extent(0,0,1,1),1,1,ncols,nrows)
      val e = IntArrayRasterData(Array[Int](5,5,5,
                                            5,1,5,
                                            5,5,5),
                            ncols,nrows)
      val e_raster = Raster(e, r_extent)
      val m = IntArrayRasterData(Array[Int](2,4,8,
                                            1,NODATA,16,
                                            128,64,32),
                            ncols,nrows)
      val m_raster = Raster(m, r_extent)
      val e_computed = FlowDirection(e_raster)
      assertEqual(e_computed, m_raster)
    }

    it("should ignore NODATA values when computing directions") {
      val ncols = 3
      val nrows = 3
      val r_extent = RasterExtent(Extent(0,0,1,1),1,1,ncols,nrows)
      val e = IntArrayRasterData(Array[Int](8,5,6,
                                            NODATA,5,6,
                                            9,6,7),
                            ncols,nrows)
      val e_raster = Raster(e, r_extent)
      val m = IntArrayRasterData(Array[Int](1,4,16,
                                            NODATA,64,16,
                                            1,64,32),
                            ncols,nrows)
      val m_raster = Raster(m, r_extent)
      val e_computed = RasterSource(e_raster).flowDirection.get
      assertEqual(e_computed, m_raster)
    }
  }}
