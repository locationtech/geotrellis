package org.gdal.scala.benchmark

import java.lang.System.currentTimeMillis

import geotrellis.raster._

import com.google.caliper.Benchmark
import com.google.caliper.Runner 
import com.google.caliper.SimpleBenchmark

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

/**
 * Extend this to create a main object which will run 'cls' (a benchmark).
 */
abstract class BenchmarkRunner(cls: java.lang.Class[_ <: Benchmark]) {
  def main(args: Array[String]): Unit = Runner.main(cls, args: _*)
}

trait GeoTiffReadBenchmark extends SimpleBenchmark {
  /**
   * Sugar to run 'f' for 'reps' number of times.
   */
  def run(reps: Int)(f: => Unit) = {
    var i = 0
    while (i < reps) { f; i += 1 }
  }

  @inline
  final def readGdal(path: String) =
    geotrellis.gdal.GdalReader.read(path)

  @inline
  final def readNative(path: String) =
    geotrellis.raster.io.geotiff.reader.GeoTiffReader(path).read().imageDirectories.head.toRaster

  @inline
  final def readGeoTools(path: String) =
    geotrellis.raster.io.GeoTiff.readRaster(path)
}

object ReadLargeUncompressed extends BenchmarkRunner(classOf[ReadLargeUncompressed])
class ReadLargeUncompressed extends GeoTiffReadBenchmark {

  val path = "../raster-test/data/aspect.tif"

  def timeGDALReadAspectTif(reps: Int) = run(reps)(gdalReadAspectTif)
  def gdalReadAspectTif = readGdal(path)

  def timeNativeReadAspectTif(reps: Int) = run(reps)(nativeReadAspectTif)
  def nativeReadAspectTif = readNative(path)

  def timeGeotoolsReadAspectTif(reps: Int) = run(reps)(geotoolsReadAspectTif)
  def geotoolsReadAspectTif = readGeoTools(path)
}

object ReadCCITTFAX3 extends BenchmarkRunner(classOf[ReadCCITTFAX3])
class ReadCCITTFAX3 extends GeoTiffReadBenchmark {

  val path = "../raster-test/data/geotiff-reader-tiffs/bilevel_CCITTFAX3.tif"

  def timeGDALCCITTFAX3(reps: Int) = run(reps)(gdalCCITTFAX3)
  def gdalCCITTFAX3 = readGdal(path)

  def timeNativeCCITTFAX3(reps: Int) = run(reps)(nativeCCITTFAX3)
  def nativeCCITTFAX3 = readNative(path)

  def timeGeotoolsCCITTFAX3(reps: Int) = run(reps)(geotoolsCCITTFAX3)
  def geotoolsCCITTFAX3 = readGeoTools(path)
}

object ReadCCITTFAX4 extends BenchmarkRunner(classOf[ReadCCITTFAX4])
class ReadCCITTFAX4 extends GeoTiffReadBenchmark {

  val path = "../raster-test/data/geotiff-reader-tiffs/bilevel_CCITTFAX4.tif"

  def timeGDALCCITTFAX4(reps: Int) = run(reps)(gdalCCITTFAX4)
  def gdalCCITTFAX4 = readGdal(path)

  def timeNativeCCITTFAX4(reps: Int) = run(reps)(nativeCCITTFAX4)
  def nativeCCITTFAX4 = readNative(path)

  def timeGeotoolsCCITTFAX4(reps: Int) = run(reps)(geotoolsCCITTFAX4)
  def geotoolsCCITTFAX4 = readGeoTools(path)
}

object ReadUncompressed extends BenchmarkRunner(classOf[ReadUncompressed])
class ReadUncompressed extends GeoTiffReadBenchmark {

  val path = "../raster-test/data/geotiff-reader-tiffs/bilevel.tif"

  def timeGDALUncompressed(reps: Int) = run(reps)(gdalUncompressed)
  def gdalUncompressed = readGdal(path)

  def timeNativeUncompressed(reps: Int) = run(reps)(nativeUncompressed)
  def nativeUncompressed = readNative(path)

  def timeGeotoolsUncompressed(reps: Int) = run(reps)(geotoolsUncompressed)
  def geotoolsUncompressed = readGeoTools(path)
}

object ReadTiledUncompressed extends BenchmarkRunner(classOf[ReadTiledUncompressed])
class ReadTiledUncompressed extends GeoTiffReadBenchmark {

  val path = "../raster-test/data/geotiff-reader-tiffs/bilevel_tiled.tif"

  def timeGDALTiledUncompressed(reps: Int) = run(reps)(gdalTiledUncompressed)
  def gdalTiledUncompressed = readGdal(path)

  def timeNativeTiledUncompressed(reps: Int) = run(reps)(nativeTiledUncompressed)
  def nativeTiledUncompressed = readNative(path)

  def timeGeotoolsTiledUncompressed(reps: Int) = run(reps)(geotoolsTiledUncompressed)
  def geotoolsTiledUncompressed = readGeoTools(path)
}

object ReadLZW extends BenchmarkRunner(classOf[ReadLZW])
class ReadLZW extends GeoTiffReadBenchmark {

  val path = "../raster-test/data/geotiff-reader-tiffs/econic_lzw.tif"

  def timeGDALLZW(reps: Int) = run(reps)(gdalLZW)
  def gdalLZW = readGdal(path)

  def timeNativeLZW(reps: Int) = run(reps)(nativeLZW)
  def nativeLZW = readNative(path)

  def timeGeotoolsLZW(reps: Int) = run(reps)(geotoolsLZW)
  def geotoolsLZW = readGeoTools(path)
}

object ReadPackedBits extends BenchmarkRunner(classOf[ReadPackedBits])
class ReadPackedBits extends GeoTiffReadBenchmark {

  val path = "../raster-test/data/geotiff-reader-tiffs/econic_packbits.tif"

  def timeGDALPackedBits(reps: Int) = run(reps)(gdalPackedBits)
  def gdalPackedBits = readGdal(path)

  def timeNativePackedBits(reps: Int) = run(reps)(nativePackedBits)
  def nativePackedBits = readNative(path)

  def timeGeotoolsPackedBits(reps: Int) = run(reps)(geotoolsPackedBits)
  def geotoolsPackedBits = readGeoTools(path)
}

object ReadZLib extends BenchmarkRunner(classOf[ReadZLib])
class ReadZLib extends GeoTiffReadBenchmark {

  val path = "../raster-test/data/geotiff-reader-tiffs/econic_zlib.tif"

  def timeGDALZLib(reps: Int) = run(reps)(gdalZLib)
  def gdalZLib = readGdal(path)

  def timeNativeZLib(reps: Int) = run(reps)(nativeZLib)
  def nativeZLib = readNative(path)

  def timeGeotoolsZLib(reps: Int) = run(reps)(geotoolsZLib)
  def geotoolsZLib = readGeoTools(path)
}

object ReadUncompressedESRI extends BenchmarkRunner(classOf[ReadUncompressedESRI])
class ReadUncompressedESRI extends GeoTiffReadBenchmark {

  val path = "../raster-test/data/geotiff-reader-tiffs/us_ext_clip_esri.tif"

  def timeGDALUncompressedESRI(reps: Int) = run(reps)(gdalUncompressedESRI)
  def gdalUncompressedESRI = readGdal(path)

  def timeNativeUncompressedESRI(reps: Int) = run(reps)(nativeUncompressedESRI)
  def nativeUncompressedESRI = readNative(path)

  def timeGeotoolsUncompressedESRI(reps: Int) = run(reps)(geotoolsUncompressedESRI)
  def geotoolsUncompressedESRI = readGeoTools(path)
}

object ReadUncompressedESRIStripped extends BenchmarkRunner(classOf[ReadUncompressedESRIStripped])
class ReadUncompressedESRIStripped extends GeoTiffReadBenchmark {

  val path = "../raster-test/data/geotiff-reader-tiffs/us_ext_clip_esri_stripes.tif"

  def timeGDALUncompressedESRIStripped(reps: Int) = run(reps)(gdalUncompressedESRIStripped)
  def gdalUncompressedESRIStripped = readGdal(path)

  def timeNativeUncompressedESRIStripped(reps: Int) = run(reps)(nativeUncompressedESRIStripped)
  def nativeUncompressedESRIStripped = readNative(path)

  def timeGeotoolsUncompressedESRIStripped(reps: Int) = run(reps)(geotoolsUncompressedESRIStripped)
  def geotoolsUncompressedESRIStripped = readGeoTools(path)
}

// Run this to profile with VisualVM or similar.
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

