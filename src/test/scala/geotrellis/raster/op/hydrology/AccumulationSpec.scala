package geotrellis.raster.op.hydrology

import geotrellis._
import geotrellis.raster._

import org.scalatest.FunSpec
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.junit.JUnitRunner 
import org.junit.Assert._

import geotrellis.testutil._

@org.junit.runner.RunWith(classOf[org.scalatest.junit.JUnitRunner])
class AccumulationSpec extends FunSpec 
                 with ShouldMatchers 
                 with TestServer 
                 with RasterBuilders {
				 
		describe("Accumulation"){
			it("Calulates the accumulation of water using a flow dirrection raster"){
			var ncols = 6
			var nrows = 6
			val r_extent = RasterExtent(Extent(0,0,1,1),1,1,ncols,nrows)
			val m = IntArrayRasterData(Array[Int](
											2,2,2,4,4,8,
                                            2,2,2,4,4,8,
                                            1,1,2,4,8,4,
                                            128,128,1,2,4,8,
                                            2,2,1,4,4,4,
                                            1,1,1,1,4,16),
                            ncols,nrows)
			
			val in_raster = Raster(m, r_extent)
			val o = IntArrayRasterData(Array[Int](0,0,0,0,0,0,
                                            0,1,1,2,2,0,
                                            0,3,7,5,4,0,
                                            0,0,0,20,0,1,
                                            0,0,0,1,24,0,
                                            0,2,4,7,34,1),
                            ncols,nrows)
			val out_raster = Raster(o, r_extent)
			
			assertEqual(Accumulation(in_raster),out_raster )
			}
		}
				 
				 
				 
				 }
