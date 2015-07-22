package geotrellis.vector.interpolation

import geotrellis.vector.io.json._
import spray.json.DefaultJsonProtocol._
import geotrellis.raster.interpolation.KrigingInterpolation
import geotrellis.testkit._
import geotrellis.vector._

import org.scalatest._
import spire.syntax.cfor._

class KrigingVectorInterpolationSpec extends FunSpec
with TestEngine{

  def generatePoints(pointsData : Seq[PointFeature[Double]]): Seq[PointFeature[Double]] = {
    (1 to pointsData.size).filterNot(_ % 5 == 0).map { i => pointsData(i-1)}
  }
  def generateLogPoints(pointsData : Seq[PointFeature[Double]]): Seq[PointFeature[Double]] = {
    (1 to pointsData.size).filter(_ % 1 == 0).map { i => PointFeature(pointsData(i-1).geom, math.log(pointsData(i-1).data))}
  }
  /*def generatePermutePoints(pointsData : Seq[PointFeature[Double]]): Seq[PointFeature[Double]] = {
    (1 to pointsData.size).filter( _ % 5 == 0).map { i => pointsData(i-1)}
  }
  def generatePermuteLogPoints(pointsData : Seq[PointFeature[Double]]): Seq[PointFeature[Double]] = {
    (1 to pointsData.size).filter( _ % 5 == 0).map { i => PointFeature(pointsData(i-1).geom, math.log(pointsData(i-1).data))}
  }*/

  describe("Kriging Simple Interpolation(vector) : Nickel") {
    val path = "raster-test/data/nickel.json"
    val f = scala.io.Source.fromFile(path)
    val collection = f.mkString.parseGeoJson[JsonFeatureCollection]
    f.close()
    it("should return correct prediction value") {
      val points : Seq[PointFeature[Double]] = generateLogPoints(collection.getAllPointFeatures[Double]())
      val testPointFeatures : Seq[PointFeature[Double]] = Seq{PointFeature(Point(659000, 586000), 3.0488)}
      val testPoints: Array[Point] = Array.tabulate(testPointFeatures.length){i => testPointFeatures(i).geom}
      //val sv: Double => Double = Semivariogram(points, 30000, 0, Spherical)
      val sv: Double => Double = NonLinearSemivariogram(points, 30000, 0, Spherical)
      val svParam: Array[Double] = Array(Semivariogram.r, Semivariogram.s, Semivariogram.a)
      val krigingVal: Array[(Double, Double)] = new KrigingSimple(points, 5000, svParam, Spherical).predict(testPoints)
      val E = 1e-4
      var error: List[Double] = Nil
      cfor(0)(_ < testPoints.length, _ + 1) { i =>
        //println(testPoints(i) + " => " + krigingVal(i) + " vs " + testPointFeatures(i).data)
        error = error :+ math.abs(krigingVal(i)._1 - testPointFeatures(i).data)
        krigingVal(i)._1 should be (testPointFeatures(i).data +- E)
      }
      /*val errorSum: Double = error.foldLeft(0.0)(_ + _)
      println("Error Sum = " + errorSum)
      println("Error Average = " + errorSum / testPoints.length)*/
    }
  }

  describe("Kriging Ordinary Interpolation(vector) : Nickel") {
    val path = "raster-test/data/nickel.json"
    val f = scala.io.Source.fromFile(path)
    val collection = f.mkString.parseGeoJson[JsonFeatureCollection]
    f.close()
    it("should return correct prediction value") {
      val points : Seq[PointFeature[Double]] = generateLogPoints(collection.getAllPointFeatures[Double]())
      val testPointFeatures : Seq[PointFeature[Double]] = Seq{PointFeature(Point(659000, 586000), 3.0461)}
      val testPoints: Array[Point] = Array.tabulate(testPointFeatures.length){i => testPointFeatures(i).geom}
      //val sv: Double => Double = Semivariogram(points, 30000, 0, Spherical)
      val sv: Double => Double = NonLinearSemivariogram(points, 30000, 0, Spherical)
      val svParam: Array[Double] = Array(Semivariogram.r, Semivariogram.s, Semivariogram.a)
      val krigingVal: Array[(Double, Double)] = new KrigingOrdinary(points, 5000, sv, svParam, Spherical).predict(testPoints)
      val E = 1e-4
      var error: List[Double] = Nil
      cfor(0)(_ < testPoints.length, _ + 1) { i =>
        //println(testPoints(i) + " => " + krigingVal(i) + " vs " + testPointFeatures(i).data)
        error = error :+ math.abs(krigingVal(i)._1 - testPointFeatures(i).data)
        krigingVal(i)._1 should be (testPointFeatures(i).data +- E)
      }
      /*val errorSum: Double = error.foldLeft(0.0)(_ + _)
      println("Error Sum = " + errorSum)
      println("Error Average = " + errorSum / testPoints.length)*/
    }
  }

  describe("Kriging Universal Interpolation(vector) : Cobalt") {

    val path = "raster-test/data/cobalt.json"
    val f = scala.io.Source.fromFile(path)
    val collection = f.mkString.parseGeoJson[JsonFeatureCollection]
    f.close()
    ignore("should return correct prediction value") {
      val points : Seq[PointFeature[Double]] = collection.getAllPointFeatures[Double]()
      val testPointFeatures : Seq[PointFeature[Double]] = Seq{PointFeature(Point(659000, 586000), 3.0461)}
      val testPoints: Array[Point] = Array.tabulate(testPointFeatures.length){i => testPointFeatures(i).geom}
      //val sv: Double => Double = Semivariogram(points, 0, 0, Spherical)
      val sv: Double => Double = NonLinearSemivariogram(points, 0, 0, Spherical)
      val svParam: Array[Double] = Array(Semivariogram.r, Semivariogram.s, Semivariogram.a)
      println("The cobalt values are : " + svParam.mkString(" "))
      /*val krigingVal: Array[(Double, Double)] = new KrigingOrdinary(points, 5000, sv, svParam, Spherical).predict(testPoints)
      println("krigingVal's length = " + krigingVal.length)
      val E = 1e-4
      var error: List[Double] = Nil
      cfor(0)(_ < testPoints.length, _ + 1) { i =>
        println(testPoints(i) + " => " + krigingVal(i) + " vs " + testPointFeatures(i).data)
        error = error :+ math.abs(krigingVal(i)._1 - testPointFeatures(i).data)
        krigingVal(i)._1 should be (testPointFeatures(i).data +- E)
      }*/
    }
  }
}
