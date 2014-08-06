package org.gdal.scala.benchmark

import java.lang.System.currentTimeMillis

object Bench {
  def bench[T](times:Int,body: T => Unit, v:T):Long = {
    // One warmup
    body(v)

    var i = 0
    val start = currentTimeMillis()
    while(i < times) {
      body(v)
      i += 1
    }
    val duration = currentTimeMillis() - start
    duration
  }

  def bench[T1,T2](times:Int,body: (T1,T2)=> Unit, v1:T1,v2:T2):Long = {
    var i = 0
    val start = currentTimeMillis()
    while(i < times) {
      body(v1,v2)
      i += 1
    }
    val duration = currentTimeMillis() - start
    duration
  }
}


object RasterReadBenchmark {

  def main(args: Array[String]): Unit = {
    val path = "../raster-test/data/slope.tif"
    val argPath = "../raster-test/data/data/slope.json"

    val gdalTime = Bench.bench(10, { p: String => geotrellis.gdal.GdalReader.read(path) }, path)
    val geotoolsTime = Bench.bench(10, { p: String => geotrellis.raster.io.GeoTiff.readRaster(path) }, path)
    val nativeTime = Bench.bench( 10, { p: String => 
      geotrellis.raster.io.geotiff.reader.GeoTiffReader(p).read().imageDirectories.head.toRaster 
    }, path)

    val geotrellisTime = Bench.bench(10, { p: String => 
      geotrellis.raster.io.arg.ArgReader.read(p)
    }, argPath)

    println(s"GDAL took $gdalTime ms.")
    println(s"GeoTools took $geotoolsTime ms.")
    println(s"Native reader took $nativeTime ms.")
    println(s"ARG reading took $geotrellisTime ms.")
  }
}
