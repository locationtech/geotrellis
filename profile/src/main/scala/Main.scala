package geotrellis.profile

import geotrellis._
import geotrellis.source._
import geotrellis.raster.op._
import geotrellis.raster.op.local._
import geotrellis.statistics.op.stat._
import geotrellis.io._
import geotrellis.render.op._

import geotrellis.process._

object Main {


  def getRasterExtent(name:String, w:Int, h:Int):RasterExtent = {
    val ext = RasterSource(name).info.get.rasterExtent.extent
    RasterExtent(ext,w,h)
  }


  def main(args:Array[String]):Unit = {
    val n = 4
    val names = Array("SBN_farm_mkt", "SBN_RR_stops_walk", "SBN_inc_percap", "SBN_street_den_1k")
    val weights = Array(2, 1, 5, 2)
    val colors = Array(0x0000FF, 0x0080FF, 0x00FF80, 0xFFFF00, 0xFF8000, 0xFF0000)

//    @Param(Array("64", "128", "256", "512", "1024", "2048", "4096"))
    var size:Int = 4096

    val re = getRasterExtent(names(0), size, size)
    val total = weights.sum
    val rs = (0 until n).map(i => Multiply(LoadRaster(names(i), re), weights(i)))
//    val rasterOp = global.Rescale(Divide(Add(rs: _*), total), (1, 100))
//    val rasterOp = Add(rs: _*)
    val rasterOp = Divide(Add(rs: _*),total)
    // val h = GetHistogram(rasterOp)
    // val breaksOp = GetColorBreaks(h, colors)
    // val op = RenderPng(rasterOp, breaksOp, 0)
    val op = rasterOp

    val source =
      RasterSeqSource((0 until n).map(i => RasterSource(names(i),re) * weights(i)))
//        .reduce(_+_)
        .localAdd
        .localDivide(total)
//        .rescale(1,100)
//        .renderPng(colors)


    println("Warming up GeoTrellis..")
    GeoTrellis.get(op)
    GeoTrellis.get(source)

    println("Start ops...")
    Console.readLine

    GeoTrellis.run(op) match {
      case Complete(_,hist) =>
        println(hist)
      case _ => sys.error("")
    }
    println("Done ops.")
    println("Start source...")
    Console.readLine

    GeoTrellis.run(source) match {
      case Complete(_,hist) =>
        println(hist)
      case _ => sys.error("")
    }

    println("Done.")
    GeoTrellis.shutdown
  }
}
