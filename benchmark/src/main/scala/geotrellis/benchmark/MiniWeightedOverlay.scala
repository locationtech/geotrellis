package geotrellis.benchmark

import geotrellis._
import geotrellis.source._
import geotrellis.raster.op.local._

import com.google.caliper.Param

object MiniWeightedOverlay extends BenchmarkRunner(classOf[MiniWeightedOverlay])
class MiniWeightedOverlay extends OperationBenchmark {
  @Param(Array("256", "512", "1024", "2048", "4096"))
  var size:Int = 0
  
  var ops:Op[Raster] = null
  var source:RasterSource = null

  override def setUp() {
    val r1:Raster = loadRaster("SBN_farm_mkt", size, size)
    val r2:Raster = loadRaster("SBN_RR_stops_walk", size, size)

    ops = Add(Multiply(r1, 5), Multiply(r2, 2))
    source = (RasterSource(r1) * 5) + (RasterSource(r2) * 2)
  }

  def timeMiniWeightedOverlayOps(reps:Int) = run(reps)(miniWeightedOverlayOps)
  def miniWeightedOverlayOps = get(ops)


  def timeMiniWeightedOverlaySource(reps:Int) = run(reps)(miniWeightedOverlaySource)
  def miniWeightedOverlaySource = get(source)
}

object MiniWeightedOverlayProfile {
  def getRasterExtent(name:String, w:Int, h:Int):RasterExtent = {
    val ext = RasterSource(name).info.get.rasterExtent.extent
    RasterExtent(ext,w,h)
  }
  /**
   * Loads a given raster with a particular height/width.
   */
  def loadRaster(name:String, w:Int, h:Int):Raster =
    RasterSource(name,getRasterExtent(name,w,h)).get


  import geotrellis.process._
  var size:Int = 10000
  
  var ops:Op[Raster] = null
  var source:RasterSource = null

  def main(args:Array[String]):Unit = {
    val r1:Raster = loadRaster("SBN_farm_mkt", size, size)
    val r2:Raster = loadRaster("SBN_RR_stops_walk", size, size)

    ops = Add(Multiply(r1, 5), Multiply(r2, 2))
    source = (RasterSource(r1) * 5) + (RasterSource(r2) * 2)

    println("Warming up GeoTrellis..")
    GeoTrellis.get(ops)
    GeoTrellis.get(source)

    println("Press enter to start ops...")
    System.console.readLine

    GeoTrellis.run(ops) match {
      case Complete(_,hist) =>
        println(hist)
      case _ => sys.error("")
    }
    println("Done ops.")
    println("Press enter to start source...")
    System.console.readLine

    GeoTrellis.run(source) match {
      case Complete(_,hist) =>
        println(hist)
      case _ => sys.error("")
    }

    println("Done.")
    GeoTrellis.shutdown
  }
}
