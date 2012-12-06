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

object FocalOperationsBenchmark extends MyRunner(classOf[FocalOperationsBenchmark])
class FocalOperationsBenchmark extends MyBenchmark {
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

  def timeOldHillshade(reps:Int) = run(reps)(server.run(oldfocal.Hillshade(r)))
  def timeHillshade(reps:Int) = run(reps)(server.run(focal.Hillshade(r)))

  def timeOldMax(reps:Int) = run(reps)(server.run(oldfocal.Max(r,oldfocal.Square(1))))
  def timeMax(reps:Int) = run(reps)(server.run(focal.Max(r,focal.Square(1))))

  // Mean operation, compare it against the FastFocalMean
  def timeMeanSquare1(reps:Int) = run(reps)(server.run(focal.Mean(r, focal.Square(1))))
  def timeMeanSquare3(reps:Int) = run(reps)(server.run(focal.Mean(r, focal.Square(3))))
  def timeMeanSquare7(reps:Int) = run(reps)(server.run(focal.Mean(r, focal.Square(7))))

  def timeFastMean1(reps:Int) = run(reps)(server.run(FastFocalMean(r, 1)))
  def timeFastMean3(reps:Int) = run(reps)(server.run(FastFocalMean(r, 3)))
  def timeFastMean7(reps:Int) = run(reps)(server.run(FastFocalMean(r, 7)))

  def timeMeanCircle1(reps:Int) = run(reps)(server.run(focal.Mean(r, focal.Circle(1))))
  def timeMeanCircle2(reps:Int) = run(reps)(server.run(focal.Mean(r, focal.Circle(2))))
  def timeMeanCircle3(reps:Int) = run(reps)(server.run(focal.Mean(r, focal.Circle(3))))
  def timeMeanCircle5(reps:Int) = run(reps)(server.run(focal.Mean(r, focal.Circle(5))))

  def timeOldMedian(reps:Int) = run(reps)(server.run(oldfocal.Median(r,oldfocal.Square(1))))
  def timeMedian(reps:Int) = run(reps)(server.run(focal.Median(r,focal.Square(1))))
  def timeMedianCircle1(reps:Int) = run(reps)(server.run(focal.Median(r,focal.Circle(1))))

  def timeOldMinSquare1(reps:Int) = run(reps)(server.run(oldfocal.Min(r, oldfocal.Square(1))))
  def timeOldMinSquare2(reps:Int) = run(reps)(server.run(oldfocal.Min(r, oldfocal.Square(2))))

  def timeMinSquare1(reps:Int) = run(reps)(server.run(focal.Min(r, focal.Square(1))))
  def timeMinSquare2(reps:Int) = run(reps)(server.run(focal.Min(r, focal.Square(2))))

  def timeMinCircle1(reps:Int) = run(reps)(server.run(focal.Min(r, focal.Circle(1))))
  def timeMinCircle2(reps:Int) = run(reps)(server.run(focal.Min(r, focal.Circle(2))))

  def timeOldMode(reps:Int) = run(reps)(server.run(oldfocal.Mode(r,oldfocal.Square(1))))
  def timeMode(reps:Int) = run(reps)(server.run(focal.Mode(r,focal.Square(1))))

  def timeOldMoran(reps:Int) = run(reps)(server.run(oldfocal.RasterMoransI(r,oldfocal.Square(1))))
  def timeMoranMoran(reps:Int) = run(reps)(server.run(focal.RasterMoransI(r,focal.Square(1))))

  def timeOldSum(reps:Int) = run(reps)(server.run(oldfocal.Sum(r,oldfocal.Square(1))))
  def timeSum(reps:Int) = run(reps)(server.run(focal.Sum(r,focal.Square(1))))
}
