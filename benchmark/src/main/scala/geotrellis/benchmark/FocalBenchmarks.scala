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
  def timeCursorMeanSquare1(reps:Int) = run(reps)(server.run(focal.CursorMean(r, focal.Square(1))))
  def timeCursorMeanSquare2(reps:Int) = run(reps)(server.run(focal.CursorMean(r, focal.Square(2))))
  def timeCursorMeanSquare3(reps:Int) = run(reps)(server.run(focal.CursorMean(r, focal.Square(3))))
  def timeCursorMeanSquare5(reps:Int) = run(reps)(server.run(focal.CursorMean(r, focal.Square(5))))
  def timeCursorMeanSquare7(reps:Int) = run(reps)(server.run(focal.CursorMean(r, focal.Square(7))))
  def timeCursorMeanSquare8(reps:Int) = run(reps)(server.run(focal.CursorMean(r, focal.Square(8))))
  def timeCursorMeanSquare13(reps:Int) = run(reps)(server.run(focal.CursorMean(r, focal.Square(13))))

  def timeMeanSquare1(reps:Int) = run(reps)(server.run(focal.Mean(r, focal.Square(1))))
  def timeMeanSquare2(reps:Int) = run(reps)(server.run(focal.Mean(r, focal.Square(2))))
  def timeMeanSquare3(reps:Int) = run(reps)(server.run(focal.Mean(r, focal.Square(3))))
  def timeMeanSquare5(reps:Int) = run(reps)(server.run(focal.Mean(r, focal.Square(5))))
  def timeMeanSquare7(reps:Int) = run(reps)(server.run(focal.Mean(r, focal.Square(7))))
  def timeMeanSquare8(reps:Int) = run(reps)(server.run(focal.Mean(r, focal.Square(8))))
  def timeMeanSquare13(reps:Int) = run(reps)(server.run(focal.Mean(r, focal.Square(13))))

  def timeFastMean1(reps:Int) = run(reps)(server.run(FastFocalMean(r, 1)))
  def timeFastMean2(reps:Int) = run(reps)(server.run(FastFocalMean(r, 2)))
  def timeFastMean3(reps:Int) = run(reps)(server.run(FastFocalMean(r, 3)))
  def timeFastMean5(reps:Int) = run(reps)(server.run(FastFocalMean(r, 5)))
  def timeFastMean7(reps:Int) = run(reps)(server.run(FastFocalMean(r, 7)))
  def timeFastMean8(reps:Int) = run(reps)(server.run(FastFocalMean(r, 8)))
  def timeFastMean13(reps:Int) = run(reps)(server.run(FastFocalMean(r, 13)))

  def timeCursorMeanCircle1(reps:Int) = run(reps)(server.run(focal.CursorMean(r, focal.Circle(1))))
  def timeCursorMeanCircle2(reps:Int) = run(reps)(server.run(focal.CursorMean(r, focal.Circle(2))))
  def timeCursorMeanCircle3(reps:Int) = run(reps)(server.run(focal.CursorMean(r, focal.Circle(3))))
  def timeCursorMeanCircle5(reps:Int) = run(reps)(server.run(focal.CursorMean(r, focal.Circle(5))))
  
  def timeMeanCircle1(reps:Int) = run(reps)(server.run(focal.Mean(r, focal.Circle(1))))
  def timeMeanCircle2(reps:Int) = run(reps)(server.run(focal.Mean(r, focal.Circle(2))))
  def timeMeanCircle3(reps:Int) = run(reps)(server.run(focal.Mean(r, focal.Circle(3))))
  def timeMeanCircle5(reps:Int) = run(reps)(server.run(focal.Mean(r, focal.Circle(5))))

  def timeCellwiseMedian(reps:Int) = run(reps)(server.run(oldfocal.Median(r,oldfocal.Square(1))))
  def timeCursorMedian(reps:Int) = run(reps)(server.run(focal.Median(r,focal.Square(1))))

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

  def timeCellwiseMode(reps:Int) = run(reps)(server.run(oldfocal.Mode(r,oldfocal.Square(1))))
  def timeCursorMode(reps:Int) = run(reps)(server.run(focal.Mode(r,focal.Square(1))))

  def timeCellwiseMoran(reps:Int) = run(reps)(server.run(oldfocal.RasterMoransI(r,oldfocal.Square(1))))
  def timeCursorMoran(reps:Int) = run(reps)(server.run(focal.RasterMoransI(r,focal.Square(1))))

  def timeCellwiseSum(reps:Int) = run(reps)(server.run(oldfocal.Sum(r,oldfocal.Square(1))))
  def timeCursorSum(reps:Int) = run(reps)(server.run(focal.Sum(r,focal.Square(1))))
}
