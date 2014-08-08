package org.gdal.scala.benchmark

import java.lang.System.currentTimeMillis

import scala.io._

object Bench {
  def bench[T](name: String, times:Int,body: T => Unit, v:T):Long = {
    // One warmup
    body(v)

    var i = 0
    var total = 0L
    val start = currentTimeMillis()
    while(i < times) {
      val start = currentTimeMillis()
      body(v)
      val d = currentTimeMillis() - start
      println(s"[$name] Run $i took $d ms.")
      total += d
      i += 1
    }

    total / times
  }

  def bench[T1,T2](name: String, times:Int,body: (T1,T2)=> Unit, v1:T1,v2:T2):Long = {
    // One warmup
    body(v1,v2)

    var i = 0
    var total = 0L
    val start = currentTimeMillis()
    while(i < times) {
      val start = currentTimeMillis()
      body(v1,v2)
      val d = currentTimeMillis() - start
      println(s"[$name] Run $i took $d ms.")
      total += d
      i += 1
    }

    total / times
  }
}


object RasterReadBenchmark {

  def main(args: Array[String]): Unit = {
    val path = "../raster-test/data/aspect.tif"

    val runTimes = 5

    val gdalTime = Bench.bench("gdal", runTimes, { p: String => geotrellis.gdal.GdalReader.read(path) }, path)

    val nativeTime = Bench.bench("native", runTimes, { p: String => 
      geotrellis.raster.io.geotiff.reader.GeoTiffReader(p).read().imageDirectories.head.toRaster 
    }, path)

    val geotoolsTime = Bench.bench("geotools", runTimes, { p: String => geotrellis.raster.io.GeoTiff.readRaster(path) }, path)

    // val justReadTime = Bench.bench("just read source", runTimes, { p: String => 
    //   Source.fromFile(p)(Codec.ISO8859).map(_.toByte) }, path)

    // val toBufferSource = Bench.bench("read source to buffer source", runTimes, { p: String => 
    //   Source.fromFile(p)(Codec.ISO8859).map(_.toByte).toArray }, path)

    println(s"GDAL took $gdalTime ms.")
//    println(s"GeoTools took $geotoolsTime ms.")
    println(s"Native reader took $nativeTime ms.")
    // println(s"Just reading from source took $justReadTime ms.")
    // println(s"Just converting to buffersource took $toBufferSource ms.")
  }
}


object RasterReadProfile {

  def main(args: Array[String]): Unit = {
    val path = "../raster-test/data/aspect.tif"

    println("Starting...")
    var i = 1
    while(true) {
      println(s"Reading $i...")
      val x = geotrellis.raster.io.geotiff.reader.GeoTiffReader(path).read().imageDirectories.head.toRaster
      println(s"  got $x")
      i += 1
    }

  }
}
