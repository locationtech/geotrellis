package geotrellis.vector.interpolation

import geotrellis.testkit._
import geotrellis.vector.{Linear, Point, PointFeature, Extent}

import org.scalatest._

class KrigingVectorInterpolationSpec extends FunSpec
with TestEngine{

  val points = Seq[PointFeature[Int]](
    PointFeature(Point(0.0,0.0),10),
    PointFeature(Point(1.0,0.0),20),
    PointFeature(Point(4.0,4.0),60),
    PointFeature(Point(0.0,6.0),80)
  )
  val radius = Some(6)
  val lag = 2
  val chunkSize = 100
  val pointPredict = Point(1,1)
  def krigingFunc = KrigingVectorInterpolation(KrigingVectorSimple, points, radius, chunkSize, lag, Linear)

  describe("Kriging Simple Interpolation(vector)") {

    val E = 1e-4
    val krigingSimpleTuple = krigingFunc(pointPredict)

    it("should return correct prediction value") {
      println("Prediction is " + krigingSimpleTuple._1)
      krigingSimpleTuple._1 should be (25.8681 +- E)
    }

    it("should return correct prediction variance") {
      println("Prediction variance is " + krigingSimpleTuple._2)
      krigingSimpleTuple._2 should be (8.2382 +- E)
    }
  }
}
