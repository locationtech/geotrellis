package geotrellis.benchmark

import geotrellis._
import geotrellis.process._
import geotrellis.raster.op._

import com.google.caliper.Benchmark
import com.google.caliper.Param
import com.google.caliper.Runner 
import com.google.caliper.SimpleBenchmark

import scala.math.{min, max}
import scala.util.Random

object FocalConwayOperation extends MyRunner(classOf[FocalConwayOperation])
class FocalConwayOperation extends MyBenchmark {
  var r:Raster = null

  override def setUp() {
    val path = "src/main/resources/sbn/SBN_inc_percap.arg"
    server = TestServer()

    val e = Extent(-8475497.88485957, 4825540.69147447,
                   -8317922.884859569, 4954765.69147447)
    val re = RasterExtent(e, 75.0, 75.0, 2101, 1723)
    r = server.run(io.LoadFile(path, re))
  }

  def timeOldConway(reps:Int) = run(reps)(server.run(oldfocal.Conway(r)))
  def timeConway(reps:Int) = run(reps)(server.run(focal.Conway(r)))
}

object FocalHillshadeOperation extends MyRunner(classOf[FocalHillshadeOperation])
class FocalHillshadeOperation extends MyBenchmark {
  var r:Raster = null

  override def setUp() {
    val path = "src/main/resources/sbn/SBN_inc_percap.arg"
    server = TestServer()

    val e = Extent(-8475497.88485957, 4825540.69147447,
                   -8317922.884859569, 4954765.69147447)
    val re = RasterExtent(e, 75.0, 75.0, 2101, 1723)
    r = server.run(io.LoadFile(path, re))
  }

  def timeOldHillshade(reps:Int) = run(reps)(server.run(oldfocal.Hillshade(r)))
  def timeHillshade(reps:Int) = run(reps)(server.run(focal.Hillshade(r)))
}

object FocalMaxOperation extends MyRunner(classOf[FocalMaxOperation])
class FocalMaxOperation extends MyBenchmark {
  var r:Raster = null

  override def setUp() {
    val path = "src/main/resources/sbn/SBN_inc_percap.arg"
    server = TestServer()

    val e = Extent(-8475497.88485957, 4825540.69147447,
                   -8317922.884859569, 4954765.69147447)
    val re = RasterExtent(e, 75.0, 75.0, 2101, 1723)
    r = server.run(io.LoadFile(path, re))
  }

  def timeOldMax(reps:Int) = run(reps)(server.run(oldfocal.Max(r,oldfocal.Square(1))))
  def timeMax(reps:Int) = run(reps)(server.run(focal.Max(r,focal.Square(1))))
}

object FocalMeanOperation extends MyRunner(classOf[FocalMeanOperation])
class FocalMeanOperation extends MyBenchmark {
  var r:Raster = null

  override def setUp() {
    val path = "src/main/resources/sbn/SBN_inc_percap.arg"
    server = TestServer()

    val e = Extent(-8475497.88485957, 4825540.69147447,
                   -8317922.884859569, 4954765.69147447)
    val re = RasterExtent(e, 75.0, 75.0, 2101, 1723)
    r = server.run(io.LoadFile(path, re))
  }

  def timeCellwiseMean(reps:Int) = run(reps)(server.run(focal.Mean(r,focal.Square(1))))
  def timeCursorMean(reps:Int) = run(reps)(server.run(focal.CursorMean(r,focal.Square(1))))
}

object FocalMedianOperation extends MyRunner(classOf[FocalMedianOperation])
class FocalMedianOperation extends MyBenchmark {
  var r:Raster = null

  override def setUp() {
    val path = "src/main/resources/sbn/SBN_inc_percap.arg"
    server = TestServer()

    val e = Extent(-8475497.88485957, 4825540.69147447,
                   -8317922.884859569, 4954765.69147447)
    val re = RasterExtent(e, 75.0, 75.0, 2101, 1723)
    r = server.run(io.LoadFile(path, re))
  }

  def timeCellwiseMedian(reps:Int) = run(reps)(server.run(oldfocal.Median(r,oldfocal.Square(1))))
  def timeCursorMedian(reps:Int) = run(reps)(server.run(focal.Median(r,focal.Square(1))))
}

object FocalMinOperation extends MyRunner(classOf[FocalMinOperation])
class FocalMinOperation extends MyBenchmark {
  var r:Raster = null

  override def setUp() {
    val path = "src/main/resources/sbn/SBN_inc_percap.arg"
    server = TestServer()

    val e = Extent(-8475497.88485957, 4825540.69147447,
                   -8317922.884859569, 4954765.69147447)
    val re = RasterExtent(e, 75.0, 75.0, 2101, 1723)
    r = server.run(io.LoadFile(path, re))
  }

  def timeOldMinSquared1(reps:Int) = run(reps)(server.run(oldfocal.Min(r, oldfocal.Square(1))))
  def timeOldMinSquared(reps:Int) = run(reps)(server.run(oldfocal.Min(r, oldfocal.Square(2))))
  def timeOldMinSquared3(reps:Int) = run(reps)(server.run(oldfocal.Min(r, oldfocal.Square(3)))) 

  def timeMinSquare1(reps:Int) = run(reps)(server.run(focal.Min(r, focal.Square(1))))
  def timeMinSquare(reps:Int) = run(reps)(server.run(focal.Min(r, focal.Square(2))))
  def timeMinSquare3(reps:Int) = run(reps)(server.run(focal.Min(r, focal.Square(3))))

  def timeOldMinCircle1(reps:Int) = run(reps)(server.run(oldfocal.Min(r, oldfocal.Circle(1))))
  def timeOldMinCircle2(reps:Int) = run(reps)(server.run(oldfocal.Min(r, oldfocal.Circle(2))))
  def timeOldMinCircle3(reps:Int) = run(reps)(server.run(oldfocal.Min(r, oldfocal.Circle(3))))

  def timeMinCircle1(reps:Int) = run(reps)(server.run(focal.Min(r, focal.Circle(1))))
  def timeMinCircle2(reps:Int) = run(reps)(server.run(focal.Min(r, focal.Circle(2))))
  def timeMinCircle3(reps:Int) = run(reps)(server.run(focal.Min(r, focal.Circle(3))))
}

object FocalModeOperation extends MyRunner(classOf[FocalModeOperation])
class FocalModeOperation extends MyBenchmark {
  var r:Raster = null

  override def setUp() {
    val path = "src/main/resources/sbn/SBN_inc_percap.arg"
    server = TestServer()

    val e = Extent(-8475497.88485957, 4825540.69147447,
                   -8317922.884859569, 4954765.69147447)
    val re = RasterExtent(e, 75.0, 75.0, 2101, 1723)
    r = server.run(io.LoadFile(path, re))
  }

  def timeCellwiseMode(reps:Int) = run(reps)(server.run(oldfocal.Mode(r,oldfocal.Square(1))))
  def timeCursorMode(reps:Int) = run(reps)(server.run(focal.Mode(r,focal.Square(1))))
}

object FocalMoranOperation extends MyRunner(classOf[FocalMoranOperation])
class FocalMoranOperation extends MyBenchmark {
  var r:Raster = null

  override def setUp() {
    val path = "src/main/resources/sbn/SBN_inc_percap.arg"
    server = TestServer()

    val e = Extent(-8475497.88485957, 4825540.69147447,
                   -8317922.884859569, 4954765.69147447)
    val re = RasterExtent(e, 75.0, 75.0, 2101, 1723)
    r = server.run(io.LoadFile(path, re))
  }

  def timeCellwiseMoran(reps:Int) = run(reps)(server.run(oldfocal.RasterMoransI(r,oldfocal.Square(1))))
  def timeCursorMoran(reps:Int) = run(reps)(server.run(focal.RasterMoransI(r,focal.Square(1))))
}

object FocalSumOperation extends MyRunner(classOf[FocalSumOperation])
class FocalSumOperation extends MyBenchmark {
  var r:Raster = null

  override def setUp() {
    val path = "src/main/resources/sbn/SBN_inc_percap.arg"
    server = TestServer()

    val e = Extent(-8475497.88485957, 4825540.69147447,
                   -8317922.884859569, 4954765.69147447)
    val re = RasterExtent(e, 75.0, 75.0, 2101, 1723)
    r = server.run(io.LoadFile(path, re))
  }

  def timeCellwiseSum(reps:Int) = run(reps)(server.run(oldfocal.Sum(r,oldfocal.Square(1))))
  def timeCursorSum(reps:Int) = run(reps)(server.run(focal.Sum(r,focal.Square(1))))
}
