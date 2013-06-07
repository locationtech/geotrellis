package geotrellis.process

import org.scalatest.FunSpec
import org.scalatest.matchers.ShouldMatchers

import geotrellis._
import geotrellis.raster._
import geotrellis.testutil._

class TileSetRasterLayerSpec extends FunSpec 
                                with ShouldMatchers 
                                with TestServer {

describe("TileSetRasterLayer") {
    it("can cache") {
      println("Loading tiles...") 
      val start = System.nanoTime
      val uncached = run(io.LoadRaster("mtsthelens_tiled"))
      val cached = run(io.LoadRaster("mtsthelens_tiled_cached"))

      // Make sure it's cached.
      cached.data.asInstanceOf[TileSetRasterData].loader.asInstanceOf[CacheTileLoader]
      
      cached.toArray should be (uncached.toArray)
      val duration = System.nanoTime - start
      println(s"Cache test took ${duration / 1000000000} seconds.")
    }
  }
}
