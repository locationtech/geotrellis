package geotrellis.raster.interpolation

import com.vividsolutions.jts.math.Matrix
import geotrellis.raster._
import geotrellis.vector._
import org.apache.commons.math3.linear.{LUDecomposition, MatrixUtils, RealMatrix}
import spire.syntax.cfor._

//MLLIB library from Apache Spark can not be used, since it depends on ScalaNLP as well
//import org.apache.spark.mllib.linalg.{Vectors,Vector,Matrix,SingularValueDecomposition,DenseMatrix,DenseVector}
//import org.apache.spark._

/*
class KrigingSimpleInterpolation(method: KrigingInterpolationMethod, points: Seq[PointFeature[Int]], re: RasterExtent,
                                 radius: Option[Int], chunkSize: Int, lag:Int=0, model:ModelType)
      extends KrigingInterpolation(method, points, re, radius, chunkSize, lag, model){

  def getDistanceArray(points: Seq[PointFeature[Int]]): Array[Array[Double]] = {
    def distance(p1: Point, p2: Point) = math.abs(math.sqrt(math.pow(p1.x - p2.x,2) + math.pow(p1.y - p2.y,2)))
    val pointSize = points.size
    /*
    val distArray = Array.ofDim[Double](pointSize * pointSize)
    //val distArray1 = Array.ofDim[Double](pointSize, pointSize)
    cfor(0)(_ < pointSize, _ + 1) { i =>
        cfor(0)(_ < pointSize, _ + 1) { j =>
          //distArray(i*pointSize + j) = distance(points(i).geom, points(j).geom)
          //Array.tabulate(100,6) { (i,j) => new distArray(i*pointSize + j) }
          //distArray1.tabulate(100,6) { (i,j) => new distArray(i*pointSize + j) }
        }
    }
    //val distArray = Array.tabulate(pointSize, pointSize) { (i,j) => distance(points(i).geom, points(j).geom)}
    */
    Array.tabulate(pointSize, pointSize) { (i,j) => distance(points(i).geom, points(j).geom)}
  }

  def getCovariogramMatrix(points: Seq[PointFeature[Int]]): Array[Array[Double]] = {
    /*
    val distArray = getDistanceArray(points)
    val pointSize = points.size
    Array.tabulate(100,6) { (i,j) => new distArray(i*pointSize + j) }
    cfor(0)(_ < pointSize, _ + 1) { i =>
      cfor(0)(_ < pointSize, _ + 1) { j =>
        distArray(i*pointSize + j) = (points(i).geom.x * points(j).geom.x) + (points(i).geom.y * points(j).geom.y)
      }
    }
    */
    val pointSize = points.size
    def distance(p1: Point, p2: Point) = math.abs(math.sqrt(math.pow(p1.x - p2.x,2) + math.pow(p1.y - p2.y,2)))
    val sv = Semivariogram(points, radius, lag, model)
    val nugget: Double = sv(0)
    val sill: Double = sv(1) - nugget
    println("The sill is = " + sill)
    println("sv(0) is = " + sv(0))
    cfor(0)(_ < pointSize, _ + 1) { i =>
      cfor(0)(_ < pointSize, _ + 1) { j =>
        print(points(i).geom.x + "," + points(i).geom.y + " to ")
        print(points(j).geom.x + "," + points(j).geom.y + " = ")
        println(distance(points(i).geom, points(j).geom) + " -> " + sv(distance(points(i).geom, points(j).geom)))
      }
    }
    val distanceArray = Array.tabulate(pointSize, pointSize) { (i,j) => distance(points(i).geom, points(j).geom)}
    val covariogram = Array.tabulate(pointSize, pointSize) { (i,j) => sill - sv(distance(points(i).geom, points(j).geom))}
    println("Distance Array")
    println(distanceArray.deep.mkString("\n"))
    println("Covariogram Array")
    println(covariogram.deep.mkString("\n"))
    println(covariogram.getClass.getSimpleName)
    println(covariogram(1)(1))
    covariogram
  }

  def krigingsimple(method: KrigingInterpolationMethod, points: Seq[PointFeature[Int]], re: RasterExtent, radius: Option[Int], pointPredict: Point, chunkSize: Int, lag:Int=0, model:ModelType): Tile = {

    if(points.size == 0)
      throw new IllegalArgumentException("No Points in the observation sequence");
    model match {
      case Linear => {
        val mean: Double = (points.foldLeft(0.0)(_ + _.data)) / points.size
        /*
        val semivariogram = Semivariogram(points, radius, lag, model)
        val nugget: Double = semivariogram(0)
        val sill: Double = semivariogram(1) - nugget
        //val distanceMatrix: Matrix[Double]
        for( i <- 1 to points.size){
          for( j <- 1 to points.size){
            println( "Value of a: " + i + " " + j );
            //val doubles = byteBuffer.getDoubleArray(tagMetadata.length, tagMetadata.offset)
            //computeDistance(points)
            val matrix = Array(
              Array(doubles(0), doubles(1), doubles(2), doubles(3)),
              Array(doubles(4), doubles(5), doubles(6), doubles(7)),
              Array(doubles(8), doubles(9), doubles(10), doubles(11)),
              Array(doubles(12), doubles(13), doubles(14), doubles(15))
            )
            //val rowMatrix = Matrix()
            //val rowMatrix = new RowMatrix
          }
        }
        */
        //getCovariogramMatrix(points)
        //RealMatrix nn = MatrixUtils.createRealMatrix(matrixData)
        /*
        val nn: RealMatrix = MatrixUtils.createRealMatrix(getCovariogramMatrix(points))
        println("Lulz")
        println(nn)
        val nnInverse: RealMatrix = new LUDecomposition(nn).getSolver().getInverse()
        println("Yes")
        println(nnInverse)
        val nnInverse1: RealMatrix = new LUDecomposition(nnInverse).getSolver().getInverse()
        println("Yes")
        println(nnInverse1)
        val nnInverse2: RealMatrix = nnInverse.multiply(nnInverse1)
        println("Yes")
        println(nnInverse2)
        */
        //val estimate = find the inverse using a built-in functionality of Scala,
        //Else try out Spark(which is used in GT earlier)
      }
      case Gaussian => ???
      case Exponential => ???
      case Circular => ???
      case Spherical => ???
      case Power => ???
      case Wave => ???
    }
    ArrayTile(Array[Int](100, 100, 100, 100), 2, 2)
  }
  override def interpolateValid(point: Point): Tile = {
    krigingsimple(method, points, re, radius, point, chunkSize, lag, model)
  }
}




        println(covariogram.getColumn(0).mkString(" "))
        //println(covariogram.copySubMatrix(1, points.size - 1, 0, 0))
        //val c_matrix: RealMatrix = MatrixUtils.createRowRealMatrix()
        //val c_Array: Array[Array[Double]] //= Array.ofDim[Double](2, 2)
        //val c_Array: Array[Array[Double]] = Array.ofDim[Double](1, points.size)
        //covariogram.copySubMatrix(1, points.size - 1, 0, 0, c_matrix)
        //copySubMatrix(1, points.size - 1, 0, 0, c_matrix)
        //val c_matrix: RealMatrix = MatrixUtils.createRowRealMatrix()
*/