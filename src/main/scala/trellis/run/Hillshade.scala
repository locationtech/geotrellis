package trellis.run 

import trellis._
import trellis.operation.render.png._
import trellis.operation._
import trellis.process._
import trellis.data._

object HillshadeTest {
  def main(args:Array[String]) {
    println("Starting test Hillshade operation.")
    println("Loading raster")
    val server = TestServer()
    //val path = "src/test/resources/econic.tif"
    //val raster1 = GeoTiffReader.read(path, None, None)

    val i = LoadFile("/var/trellis/stroud/elevation30m-20110607.arg32")
    server.run(i)

    val start = System.currentTimeMillis
    println("hillshade")
    val h = Hillshade(LoadFile("/var/trellis/stroud/elevation30m-20110607.arg32"), 45.0, 20.0)
    val grayscale = for (i <- 0 until 255) yield (i, i * (256 * 256) + i * 256 + i)
    
    val png = WritePNGFile(h,"/tmp/hillshade.png", Literal(grayscale.toArray), -1, true)
    server.run(png)
    val elapsed = System.currentTimeMillis - start
    println("ran hillshade: %d".format(elapsed))

    server.shutdown()
  }
}


// vim: set ts=4 sw=4 et:
