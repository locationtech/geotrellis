package geotrellis.benchmark

import java.lang.System._

import com.google.caliper.Benchmark
import com.google.caliper.Runner
import com.google.caliper.SimpleBenchmark

object Bench {
  def bench[T](name: String, times: Int, body: T => Unit, v: T): Long = {
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

  def bench[T1, T2](name: String, times: Int, body: (T1, T2) => Unit, v1: T1,v2: T2): Long = {
    // One warmup
    body(v1, v2)

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

trait ShapeFileBenchmark extends SimpleBenchmark {
  /**
    * Sugar to run 'f' for 'reps' number of times.
    */
  def run(reps: Int)(f: => Unit) = {
    var i = 0
    while (i < reps) { f; i += 1 }
  }

  @inline
  final def readNative(path: String) =
    geotrellis.raster.io.shape.reader.ShapeFileReader(path).read

  @inline
  final def readGeoTools(path: String) =
    geotrellis.geotools.ShapeFileReader.readMultiPolygonFeatures(path)

}

object CountriesShapeFileBenchmark
    extends BenchmarkRunner(classOf[CountriesShapeFileBenchmark])

class CountriesShapeFileBenchmark extends ShapeFileBenchmark {

  val path = "../raster-test/data/shapefiles/countries/countries.shp"

  def timeNativeReadCountriesShapeFile(reps: Int) =
    run(reps)(nativeReadCountriesShapeFile)

  def nativeReadCountriesShapeFile = readNative(path)

  def timeGeoToolsReadCountriesShapeFile(reps: Int) =
    run(reps)(geoToolsReadCountriesShapeFile)

  def geoToolsReadCountriesShapeFile = readGeoTools(path)

}
