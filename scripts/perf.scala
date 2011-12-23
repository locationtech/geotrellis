#!/bin/sh
exec scala -cp $HOME/trellis/trellis/target/scala_2.8.0/classes -Xscript Script $0
!#

import trellis.operation._
import trellis.process._

import scala.math.{min,max}

val weights = Array(2, 1, 5, 2)
val paths   = Array("SBN_farm_mkt", "SBN_RR_stops_walk", "SBN_inc_percap",
                    "SBN_street_den_1k").map { "src/test/resources/sbn/" + _ }

var height = 600
var width  = 600

val maskpath    = "src/test/resources/sbn/SBN_co_phila"
val colors      = Array(0x0000FF, 0x0080FF, 0x00FF80, 0xFFFF00, 0xFF8000, 0xFF0000)
val noDataColor = 0x000000

if (args.length == 0) {
  //ok
} else if (args.length == 2) {
  width  = args(0).toInt
  height = args(1).toInt
} else {
  Console.printf("bad arguments given!\n")
  for(i <- 0 until args.length) {
    Console.printf("args(%d): %s\n", i, args(i).toString)
  }
  exit(1)
}

// xyz
val server = new Server("test")

// ACTUAL EXECUTION STARTS HERE
def run = {
  val t0 = System.currentTimeMillis()

  // pre-process the total weights
  val weightsum:Int = weights.foldLeft(0.toInt) { (a, b) => (a + b).toInt }

  val geo = LoadGeoAttrsFromArgFile(paths(0)).run(server)
  
  // load the files
  var Rs:Seq[IntRasterOperation] = paths.map {
    path => LoadArgFileChunk(path, BuildGeoAttrs(xmin = geo.xmin, ymin = geo.ymin,
                                                 xmax = geo.xmax, ymax = geo.ymax,
                                                 cols = width, rows = width,
                                                 projection=102113))
  }
  
  // associate the rasters with their weights
  val Tpls = Rs.zip { weights }
  
  // multiply each raster point by its weight
  Rs = Tpls.map { tpl => MultiplyConstant(tpl._1, tpl._2) }
  
  // average: add the rasters then divide by the sum of the weights
  val W = DivideConstant(Add(Rs: _*), weightsum)
  
  // load the mask file
  val M = LoadResampledArgFile(maskpath, height, width)
  
  // apply the mask
  val T = if (false) { Mask(W, M, 0, 0) } else { W }
  
  // normalize the result for 1-100
  val I = Immutable(Normalize(T, 1, 100))
  
  // create a histogram for color breaks
  val H = BuildHistogramArray(I)
  
  // create colorized quantile breaks
  val C = FindClassBreaks(H, colors.length)

  val classBreaks = server.run(C)
  //C.logTimingTree
  val colorBreaks = classBreaks.zip(colors).toArray

  val t1 = System.currentTimeMillis();
  val class_t = t1 - t0
  
  WritePNGFile(I, "/tmp/output.png", colorBreaks, 0, noDataColor, true).run(server)

  val t2 = System.currentTimeMillis();
  val png_t = t2 - t1
  val total_t = t2 - t0

  val tpl = (class_t, png_t, total_t)
  //println(tpl)
  tpl
}

// run a few times just to get started
(1 to 1).foreach { i => run }

// ok now run n times and log
val n = 10

val tpls = (1 to n).map { i => run }

val totals = tpls.reduceLeft {
  (a, b) => (a._1 + b._1, a._2 + b._2, a._3 + b._3)
}
val mins = tpls.reduceLeft {
  (a, b) => (min(a._1, b._1), min(a._2, b._2), min(a._3, b._3))
}
val maxs = tpls.reduceLeft {
  (a, b) => (max(a._1, b._1), max(a._2, b._2), max(a._3, b._3))
}

val avgs = (totals._1 / n, totals._2 / n, totals._3 / n)

Console.printf("class breaks  %5d ms (min %d, max %d)\n", avgs._1, mins._1, maxs._1)
Console.printf("png render    %5d ms (min %d, max %d)\n", avgs._2, mins._2, maxs._2)
Console.printf("TOTAL         %5d ms (min %d, max %d)\n", avgs._3, mins._3, maxs._3)
