package geotrellis.raster.op.tiles

import geotrellis.testutil._
import geotrellis.raster._

import org.scalatest.FunSpec
import org.scalatest.matchers.ShouldMatchers

// TODO
//@org.junit.runner.RunWith(classOf[org.scalatest.junit.JUnitRunner])
// class AsTiledRasterSpec extends FunSpec with ShouldMatchers 
//                                         with TestServer 
//                                         with RasterBuilders {
//   describe("AsTiledRaster") {
//     it("should wrap raster into tiles") {
//       val untiled = byteRaster
//       val pixelCols = 2
//       val pixelRows = 3
//       val tiled = run(AsTiledRaster(untiled,pixelCols,pixelRows))
//       tiled.isTiled should be (true)
//       val tiledData = tiled.data.asInstanceOf[TiledRasterData]
//       val tile = tiledData.getTileRaster(tiledData.tileLayout.getResolutionLayout(tiled.rasterExtent),6,2)

//       tile.get(0,0) should be (untiled.get(12,6))
//       tile.get(1,0) should be (untiled.get(13,6))
//       tile.get(0,1) should be (untiled.get(12,7))
//       tile.get(1,1) should be (untiled.get(13,7))
//       tile.get(0,2) should be (untiled.get(12,8))
//       tile.get(1,2) should be (untiled.get(13,8))
//     }
//   }
// }
