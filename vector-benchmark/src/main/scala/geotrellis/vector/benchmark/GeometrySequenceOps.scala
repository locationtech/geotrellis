package geotrellis.vector.benchmark

import geotrellis.vector._

import geotrellis.vector.check.jts.Generators

import org.scalacheck.{Test,Gen}

object GeometrySequenceOps extends BenchmarkRunner(classOf[GeometrySequenceOps])
class GeometrySequenceOps extends FeatureBenchmark {

  var ml: MultiLine = null

  override def setUp() {
    val params = Test.Parameters.default
    val genPrms = new Gen.Parameters.Default { override val rng = params.rng }

    ml = null
    while (ml == null) {
      Generators.genMultiLineString(genPrms) match {
        case None =>
        case Some(value) =>
          ml = MultiLine(value)
      }
    }
  }

  def timeMultiLineUnion(reps: Int) = run(reps)(multiLineUnion)
  def multiLineUnion = ml.union

  def timeMultiLineIntersection(reps: Int) = run(reps)(multiLineIntersection)
  def multiLineIntersection = ml.intersection

  def timeMultiLineDifference(reps: Int) = run(reps)(multiLineDifference)
  def multiLineDifference = ml.difference

  def timeMultiLineSymDifference(reps: Int) = run(reps)(multiLineSymDifference)
  def multiLineSymDifference = ml.symDifference
}

