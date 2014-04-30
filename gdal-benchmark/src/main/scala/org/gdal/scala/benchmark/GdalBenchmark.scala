package org.gdal.scala.benchmark

import java.lang.System.currentTimeMillis

import geotrellis._
import geotrellis.source._
import geotrellis.process._

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
    val path = "../core-test/data/slope.tif"
    val argPath = "../core-test/data/data/slope.json"

    val gdalTime = Bench.bench(10, { p: String => geotrellis.gdal.GdalReader.read(path) }, path)
    val geotoolsTime = Bench.bench(10, { p: String => geotrellis.data.GeoTiff.readRaster(path) }, path)
    val geotrellisTime = Bench.bench(10, { p: String => 
      RasterLayer.fromPath(p).get.getRaster
    }, argPath)

    println(s"GDAL took $gdalTime ms.")
    println(s"GeoTools took $geotoolsTime ms.")
    println(s"GeoTrellis took $geotrellisTime ms.")
  }
}
