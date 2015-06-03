package geotrellis.raster.interpolation

import geotrellis.raster._
import geotrellis.vector._
import org.apache.commons.math3.linear.{LUDecomposition, MatrixUtils, RealMatrix}

class KrigingSimpleInterpolation(method: KrigingInterpolationMethod, points: Seq[PointFeature[Int]], re: RasterExtent,
                                 radius: Option[Int], chunkSize: Int, lag:Int=0, model:ModelType)
      extends KrigingInterpolation(method, points, re, radius, chunkSize, lag, model){

  val pointSize = points.size
  def distance(p1: Point, p2: Point) = math.abs(math.sqrt(math.pow(p1.x - p2.x,2) + math.pow(p1.y - p2.y,2)))

  def getCovariogramMatrix(sv: Function1[Double,Double], points: Seq[PointFeature[Int]]): Array[Array[Double]] = {
    val sill: Double = sv(1) - sv(0)
    val covariogram = Array.tabulate(pointSize, pointSize) { (i,j) => sill - sv(distance(points(i).geom, points(j).geom))}

    covariogram
  }

  //N.B. Restructure the Kriging Classes to conform to functional programming rather than OOP
  //After this push (after implementing the Simple Kriging, reshape the code structure)

  //Spire and Breeze have not been used
  //jts library was being used, but did not seem very good; so switched to Apache Math3 library
  def krigingsimple(method: KrigingInterpolationMethod, points: Seq[PointFeature[Int]], re: RasterExtent, radius: Option[Int], pointPredict: Point, chunkSize: Int, lag:Int=0, model:ModelType): (Double, Double) = {

    if(pointSize == 0)
      throw new IllegalArgumentException("No Points in the observation sequence");
    model match {
      case Linear => {
        val mean: Double = (points.foldLeft(0.0)(_ + _.data)) / pointSize
        val sv = Semivariogram(points, radius, lag, model)

        val covariogram: RealMatrix = MatrixUtils.createRealMatrix(getCovariogramMatrix(sv, points))
        val covarianceInverse: RealMatrix = new LUDecomposition(covariogram).getSolver().getInverse()

        val nugget: Double = sv(0)
        val sill: Double = sv(1) - nugget

        val cMatrix: RealMatrix = MatrixUtils.createRealMatrix(Array.tabulate(1, pointSize) {(_,i) => sill - sv(distance(pointPredict, points(i).geom))})
        val errorMatrix: RealMatrix = MatrixUtils.createRealMatrix(Array.tabulate(pointSize, 1) {(i, _) => points(i).data - mean})
        val prediction: Double = mean + (cMatrix.multiply(covarianceInverse)).multiply(errorMatrix).getEntry(0,0)

        val krigingVariance: Double = math.sqrt(sill - (cMatrix.multiply(covarianceInverse)).multiply(cMatrix.transpose()).getEntry(0,0))
        println("The prediction is " + prediction)
        println("The variance is " + krigingVariance)

        //Do we return the range of the prediction as well
        //If so, what confidence tolerance value should we use?
        //Should that also be taken by the user or a general value of 95% be used
        def predictMin: Double = ???
        def predictMax: Double = ???

        (prediction, krigingVariance)
      }

      //For other semivarigram model handle separately
      //Since, linear semivariograms are different from others because they do not flatten out
      case _ => ???
    }
  }
  override def interpolateValid(point: Point): (Double, Double) = {
    krigingsimple(method, points, re, radius, point, chunkSize, lag, model)
  }
}
