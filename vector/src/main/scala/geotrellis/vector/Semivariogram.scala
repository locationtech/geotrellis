/*
 * Copyright (c) 2014 Azavea.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package geotrellis.vector

import scala.collection.mutable
import org.apache.commons.math3.stat.regression.SimpleRegression
import org.apache.commons.math3.analysis.{MultivariateMatrixFunction, MultivariateVectorFunction}
import org.apache.commons.math3.fitting.leastsquares.LeastSquaresOptimizer.Optimum
import org.apache.commons.math3.fitting.leastsquares.{LevenbergMarquardtOptimizer, LeastSquaresBuilder, LeastSquaresProblem}
import org.apache.commons.math3.linear.DiagonalMatrix
import spire.syntax.cfor._

abstract sealed class ModelType

case object Linear extends ModelType
case object Gaussian extends ModelType
case object Circular extends ModelType
case object Spherical extends ModelType
case object Exponential extends ModelType
case object Wave extends ModelType

/**
  Empirical semivariogram
  */
object Semivariogram {
  var r: Double = 0
  var s: Double = 0
  var a: Double = 0

  case class Bucket(start:Double,end:Double) {
    //private val points = mutable.Set[(PointFeature[Int],PointFeature[Int])]()
    private val points = mutable.Set[(PointFeature[Double],PointFeature[Double])]()
    //private val pointsDouble = mutable.Set[(PointFeature[Double],PointFeature[Double])]()

    //def add(x:PointFeature[Int],y:PointFeature[Int]) = points += ((x,y))
    //def add(x:PointFeature[Double],y:PointFeature[Double]) = pointsDouble += ((x,y))
    //def add[T](x:PointFeature[T],y:PointFeature[T]) = points += ((x,y))
    def add(x:PointFeature[Double],y:PointFeature[Double]) = points += ((x,y))

    def contains(x:Double) =
      if(start==end) x == start
      else (start <= x) && (x < end)

    def midpoint = (start + end) / 2.0
    def isEmpty = points.isEmpty
    def semivariance = {
      val sumOfSquares =
        points.foldLeft(0.0){ case(acc,(x,y)) =>
          acc + math.pow(x.data-y.data,2)
        }
      (sumOfSquares / points.size) / 2
    }
  }

  /** Produces unique pairs of points */
  def makePairs[T](elements:List[T]):List[(T,T)] = {
    def f(elements:List[T],acc:List[(T,T)]):List[(T,T)] =
      elements match {
        case head :: List() =>
          acc
        case head :: tail =>
          f(tail,tail.map((head,_)) ++ acc)
        case _ => acc
      }
    f(elements,List[(T,T)]())
  }

  def explicitGaussian(r: Double, s: Double, a: Double): Double => Double = {
    h: Double => {
      if (h == 0) 0
      else
        a + (s - a) * (1 - math.exp(- math.pow(h, 2) / math.pow(r, 2)))
    }
    /*                    | 0                             . h = 0
     *  gamma(h; r,s,a) = |
     *                    | a + (s-a) {1 - e^(-h^2/r^2)}  , h > 0
     */
  }

  def explicitGaussianNugget(r: Double, s: Double): Double => Double = {
    h: Double => {
      if (h == 0) 0
      else
        s * (1 - math.exp(- math.pow(h, 2) / math.pow(r, 2)))
    }
    /*                  | 0                             . h = 0
     *  gamma(h; r,s) = |
     *                  | s {1 - e^(-h^2/r^2)}          , h > 0
     */
  }

  def jacobianGaussian(variables: Array[Double]): Double => Array[Double] = {
    var jacobianRet: Array[Double] = Array.ofDim[Double](3)
    x: Double => {
      if (x == 0)
        jacobianRet = Array.fill[Double](3)(0)
      else {
        jacobianRet(0) = (variables(2) - variables(1)) * (2 * math.pow(x, 2) / math.pow(variables(0), 3)) * math.exp(-1 * math.pow(x, 2) / math.pow(variables(0), 2))
        jacobianRet(1) = 1 - math.exp(-1 * math.pow(x, 2) / math.pow(variables(0), 2))
        jacobianRet(2) = 1 - jacobianRet(1)
      }
      jacobianRet
    }
  }

  def jacobianGaussianNugget(variables: Array[Double]): Double => Array[Double] = {
    var jacobianRet: Array[Double] = Array.ofDim[Double](2)
    x: Double => {
      if (x == 0)
        jacobianRet = Array.fill[Double](2)(0)
      else {
        jacobianRet(0) = variables(1) * (2 * math.pow(x, 2) / math.pow(variables(0), 3)) * math.exp(-1 * math.pow(x, 2) / math.pow(variables(0), 2))
        jacobianRet(1) = 1 - math.exp(-1 * math.pow(x, 2) / math.pow(variables(0), 2))
      }
      jacobianRet
    }
  }

  def explicitCircular(r: Double, s: Double, a: Double): Double => Double = {
    h: Double => {
      if (h == 0) 0
      else if (h > r)
        s
      else
        a + (s - a) * (1 - (2 / math.Pi) * math.acos(h/r) + ((2 * h) / (math.Pi * r)) * math.sqrt(1 - math.pow(h, 2) / math.pow(r, 2)))
    }
    /*                    | 0                                                                      , h = 0
     *                    |
     *                    |             |                                              _________ |
     *                    |             |      2                | h |      2h         /    h^2   |
     *  gamme(h; r,s,a) = | a + (s-a) * |1 - ----- * cos_inverse|---| + -------- *   /1 - -----  | , 0 < h <= r
     *                    |             |      pi               | r |    pi * r    \/      r^2   |
     *                    |             |                                                        |
     *                    |
     *                    | s                                                                      , h > r
     */
  }

  def explicitCircularNugget(r: Double, s: Double): Double => Double = {
    h: Double => {
      if (h == 0) 0
      else if (h > r)
        s
      else
        //s * (1 - (2 / math.Pi) * math.acos(h/r) + math.sqrt(1 - math.pow(h, 2) / math.pow(r, 2)))
        s * (1 - (2 / math.Pi) * math.acos(h/r) + ((2 * h) / (math.Pi * r)) * math.sqrt(1 - math.pow(h, 2) / math.pow(r, 2)))
    }

    /*                  | 0                                                              , h = 0
     *                  |
     *                  |     |                                              _________ |
     *                  |     |      2                | h |      2h         /    h^2   |
     *  gamme(h; r,s) = | s * |1 - ----- * cos_inverse|---| + -------- *   /1 - -----  | , 0 < h <= r
     *                  |     |      pi               | r |    pi * r    \/      r^2   |
     *                  |     |                                                        |
     *                  |
     *                  | s                                                              , h > r
     */
  }

  def jacobianCircular(variables: Array[Double]): Double => Array[Double] = {
    var jacobianRet: Array[Double] = Array.ofDim[Double](3)
    x: Double => {
      if (x == 0)
        jacobianRet = Array.fill[Double](3)(0)
      else {
        /*
        jacobianRet(0) = (-2 * (variables(1) - variables(2)) * x) / (math.Pi * math.pow(variables(0), 2)) + ((variables(1) - variables(2)) * math.pow(x, 2)) / (math.pow(variables(0), 2) * math.sqrt(1 - math.pow(x / variables(0), 2)))
        jacobianRet(1) = 1 - (2 / math.Pi) * math.acos(x / variables(0)) + math.sqrt(1 - math.pow(x / variables(0), 2))
        jacobianRet(2) = 1 - jacobianRet(1)
        */
        jacobianRet(0) = -4 * x * (variables(1) - variables(2)) * math.sqrt(math.pow(variables(0), 2) - math.pow(x, 2)) / (math.Pi * math.pow(variables(0), 3))
        jacobianRet(1) = 1 - (2 / math.Pi) * math.acos(x / variables(0)) + ((2 * x) / (math.Pi * variables(0))) * math.sqrt(1 - math.pow(x / variables(0), 2))
        jacobianRet(2) = 1 - jacobianRet(1)
      }
      jacobianRet
    }
  }

  def jacobianCircularNugget(variables: Array[Double]): Double => Array[Double] = {
    var jacobianRet: Array[Double] = Array.ofDim[Double](2)
    x: Double => {
      if (x == 0)
        jacobianRet = Array.fill[Double](2)(0)
      else {
        /*
        jacobianRet(0) = (-2 * variables(1) * x) / (math.Pi * math.pow(variables(0), 2)) + (variables(1) * math.pow(x, 2)) / (math.pow(variables(0), 2) * math.sqrt(1 - math.pow(x / variables(0), 2)))
        jacobianRet(1) = 1 - (2 / math.Pi) * math.acos(x / variables(0)) + math.sqrt(1 - math.pow(x / variables(0), 2))
        */
        jacobianRet(0) = -4 * x * variables(1) * math.sqrt(math.pow(variables(0), 2) - math.pow(x, 2)) / (math.Pi * math.pow(variables(0), 3))
        jacobianRet(1) = 1 - (2 / math.Pi) * math.acos(x / variables(0)) + ((2 * x) / (math.Pi * variables(0))) * math.sqrt(1 - math.pow(x / variables(0), 2))
      }
      jacobianRet
    }
  }

  def explicitSpherical(r: Double, s: Double, a: Double): Double => Double = {
    h: Double => {
      if (h == 0) 0
      else if (h > r) s
      else {
        a + (s - a) * ((3 * h / (2 * r)) - (math.pow(h, 3) / (2 * math.pow(r, 3)) ))
      }
    }
    /*                    | 0                           . h = 0
     *                    |           | 3h      h^3   |
     *  gamma(h; r,s,a) = | a + (s-a) |---- - ------- | , 0 < h <= r
     *                    |           | 2r     2r^3   |
     *                    | s                           , h > r
     */
  }

  def explicitSphericalNugget(r: Double, s: Double): Double => Double = {
    h: Double => {
      if (h == 0) 0
      else if (h > r) s
      else {
        s * ((3 * h / (2 * r)) - (math.pow(h, 3) / (2 * math.pow(r, 3)) ))
      }
    }
    /*                  | 0                    . h = 0
     *                  |    | 3h      h^3   |
     *  gamma(h; r,s) = | s  |---- - ------- | , 0 < h <= r
     *                  |    | 2r     2r^3   |
     *                  | s                    , h > r
     */
  }

  def jacobianSpherical(variables: Array[Double]): Double => Array[Double] = {
    var jacobianRet: Array[Double] = Array.ofDim[Double](3)
    x: Double => {
      if (x == 0)
        jacobianRet = Array.fill[Double](3)(0)
      else if (x>0 && x<=variables(0)) {
        jacobianRet(0) = (variables(1) - variables(2)) * ((-3*x)/(2*math.pow(variables(0),2)) + (3 * math.pow(x,3)/(2 * math.pow(variables(0),4))))
        jacobianRet(1) = ((3 * x)/(2 * variables(0))) - (0.5 * math.pow(x/variables(0),3))
        jacobianRet(2) = 1 - jacobianRet(1)
      }
      else
        jacobianRet = Array[Double](0, 1, 0)
      jacobianRet
    }
  }

  def jacobianSphericalNugget(variables: Array[Double]): Double => Array[Double] = {
    var jacobianRet: Array[Double] = Array.ofDim[Double](2)
    x: Double => {
      if (x == 0)
        jacobianRet = Array.fill[Double](2)(0)
      else if (x>0 && x<=variables(0)) {
        jacobianRet(0) = variables(1) * ((-3*x)/(2*math.pow(variables(0),2)) + (3 * math.pow(x,3)/(2 * math.pow(variables(0),4))))
        jacobianRet(1) = ((3 * x)/(2 * variables(0))) - (0.5 * math.pow(x/variables(0),3))
      }
      else
        jacobianRet = Array[Double](0, 1)
      jacobianRet
    }
  }

  def explicitExponential(r: Double, s: Double, a: Double): Double => Double = {
    h: Double => {
      if (h == 0) 0
      else
        a + (s - a) * (1 - math.exp(- 3 * h / r))
    }
    /*                    | 0                           . h = 0
     *  gamma(h; r,s,a) = |
     *                    | a + (s-a) {1 - e^(-3h/r)}   , h > 0
     */
  }

  def explicitExponentialNugget(r: Double, s: Double): Double => Double = {
    h: Double => {
      if (h == 0) 0
      else
        s * (1 - math.exp(- 3 * h / r))
    }
    /*                  | 0                   . h = 0
     *  gamma(h; r,s) = |
     *                  | s {1 - e^(-3h/r)}   , h > 0
     */
  }

  def jacobianExponential(variables: Array[Double]): Double => Array[Double] = {
    var jacobianRet: Array[Double] = Array.ofDim[Double](3)
    x: Double => {
      if (x == 0)
        jacobianRet = Array.fill[Double](3)(0)
      else {
        jacobianRet(0) = (variables(2) - variables(1)) * (3 * x / math.pow(variables(0), 2)) * math.exp(-3 * x / variables(0))
        jacobianRet(1) = 1 - math.exp(-3 * x / variables(0))
        jacobianRet(2) = 1 - jacobianRet(1)
      }
      jacobianRet
    }
  }

  def jacobianExponentialNugget(variables: Array[Double]): Double => Array[Double] = {
    var jacobianRet: Array[Double] = Array.ofDim[Double](2)
    x: Double => {
      if (x == 0)
        jacobianRet = Array.fill[Double](2)(0)
      else {
        jacobianRet(0) = -1 * variables(1) * (3 * x / math.pow(variables(0), 2)) * math.exp(-3 * x / variables(0))
        jacobianRet(1) = 1 - math.exp(-3 * x / variables(0))
      }
      jacobianRet
    }
  }

  def explicitWave(w: Double, s: Double, a: Double): Double => Double = {
    h: Double => {
      if (h == 0) 0
      else
        a + (s - a) * (1 - w * math.sin(h / w) / h)
    }
    /*                    | 0                             . h = 0
     *                    |
     *  gamma(h; w,s,a) = |           |       sin(h/w)  |
     *                    | a + (s-a) |1 - w ---------- | , h > 0
     *                    |           |         h       |
     */
    //N.B. The terms are in degrees. not radians
  }

  def explicitWaveNugget(w: Double, s: Double): Double => Double = {
    h: Double => {
      if (h == 0) 0
      else
        s * (1 - w * math.sin(h / w) / h)
    }
    /*                  | 0                      . h = 0
     *                  |
     *  gamma(h; w,s) = |    |       sin(h/w)  |
     *                  | s  |1 - w ---------- | , h > 0
     *                  |    |         h       |
     */
  }

  def jacobianWave(variables: Array[Double]): Double => Array[Double] = {
    var jacobianRet: Array[Double] = Array.ofDim[Double](3)
    x: Double => {
      if (x == 0)
        jacobianRet = Array.fill[Double](3)(0)
      else {
        jacobianRet(0) = (variables(1) - variables(2)) * ((math.cos(x/variables(0))/variables(0)) - (math.sin(x/variables(0))/x))
        jacobianRet(1) = 1 - variables(0) * math.sin(x / variables(0)) / x
        jacobianRet(2) = 1 - jacobianRet(1)
      }
      jacobianRet
    }
  }

  def jacobianWaveNugget(variables: Array[Double]): Double => Array[Double] = {
    var jacobianRet: Array[Double] = Array.ofDim[Double](2)
    x: Double => {
      if (x == 0)
        jacobianRet = Array.fill[Double](2)(0)
      else {
        jacobianRet(0) = variables(1) * ((math.cos(x/variables(0))/variables(0)) - (math.sin(x/variables(0))/x))
        jacobianRet(1) = 1 - variables(0) * math.sin(x / variables(0)) / x
      }
      jacobianRet
    }
  }

  trait LeastSquaresFittingProblem {
    var x: Array[Double] = Array()
    var y: Array[Double] = Array()
    var start: Array[Double] = Array()

    def addPoint(Px: Double, Py: Double) = {
      x = x :+ Px
      y = y :+ Py
    }
    def calculateTarget(): Array[Double] = y
    def valueFunc(w: Double, s: Double, a: Double): Double => Double
    def jacobianFunc(variables: Array[Double]): Double => Array[Double]

    def retMVF(): MultivariateVectorFunction = {
      new MultivariateVectorFunction {
        def value(variables: Array[Double]): Array[Double] = {
          val values: Array[Double] = Array.ofDim[Double](x.length)
          cfor(0)(_ < values.length, _ + 1) { i =>
            values(i) = valueFunc(variables(0), variables(1), variables(2))(x(i))
          }
          /*println("X array : ")
          println(x.mkString(" "))
          println("Parameters : ")
          println(variables.mkString(" "))
          println("MVF")
          println(values.mkString(" "))*/
          values
        }
      }
    }
    def retMMF(): MultivariateMatrixFunction = {
      def jacobianConstruct(variables: Array[Double]): Array[Array[Double]] = {
        val jacobianRet: Array[Array[Double]] = Array.ofDim[Double](x.length, 3)
        cfor(0)(_ < jacobianRet.length, _ + 1) { i =>
          jacobianRet(i) = jacobianFunc(variables)(x(i))
        }
        /*println("MMF")
        //println(jacobianRet.mkString("\n"))
        cfor(0)(_ < jacobianRet.length, _ + 1) { i =>
          println(jacobianRet(i).mkString(" "))
        }*/
        jacobianRet
      }
      new MultivariateMatrixFunction {
        override def value(doubles: Array[Double]): Array[Array[Double]] = jacobianConstruct(doubles)
      }
    }
  }

  trait LeastSquaresFittingNuggetProblem {
    var x: Array[Double] = Array()
    var y: Array[Double] = Array()
    var start: Array[Double] = Array()

    def addPoint(Px: Double, Py: Double) = {
      x = x :+ Px
      y = y :+ Py
    }
    def calculateTarget(): Array[Double] = y

    def valueFuncNugget(w: Double, s: Double): Double => Double
    def jacobianFuncNugget(variables: Array[Double]): Double => Array[Double]

    def retMVF(): MultivariateVectorFunction = {
      new MultivariateVectorFunction {
        def value(variables: Array[Double]): Array[Double] = {
          val values: Array[Double] = Array.ofDim[Double](x.length)
          cfor(0)(_ < values.length, _ + 1) { i =>
            values(i) = valueFuncNugget(variables(0), variables(1))(x(i))
          }
          values
        }
      }
    }
    def retMMF(): MultivariateMatrixFunction = {
      def jacobianConstruct(variables: Array[Double]): Array[Array[Double]] = {
        val jacobianRet: Array[Array[Double]] = Array.ofDim[Double](x.length, 2)
        cfor(0)(_ < jacobianRet.length, _ + 1) { i =>
          jacobianRet(i) = jacobianFuncNugget(variables)(x(i))
        }
        jacobianRet
      }
      new MultivariateMatrixFunction {
        override def value(doubles: Array[Double]): Array[Array[Double]] = jacobianConstruct(doubles)
      }
    }
  }

  def optConstructor(problem: AnyRef) = {
    val lsb: LeastSquaresBuilder = new LeastSquaresBuilder()
    val lmo: LevenbergMarquardtOptimizer = new LevenbergMarquardtOptimizer()
    problem match {
      case problem: LeastSquaresFittingProblem =>
        lsb.model(problem.retMVF(), problem.retMMF())
        lsb.target(problem.calculateTarget())
        lsb.start(problem.start)
        lsb.maxEvaluations(Int.MaxValue)
        lsb.maxIterations(Int.MaxValue)

        val lsp: LeastSquaresProblem = lsb.build
        lmo.optimize(lsp)

      case problem: LeastSquaresFittingNuggetProblem =>
        lsb.model(problem.retMVF(), problem.retMMF())
        lsb.target(problem.calculateTarget())
        lsb.start(problem.start)
        lsb.maxEvaluations(Int.MaxValue)
        lsb.maxIterations(Int.MaxValue)

        val lsp: LeastSquaresProblem = lsb.build
        lmo.optimize(lsp)
    }
  }

  def printOptimization(opt: Optimum) = {
    val optimalValues = opt.getPoint.toArray
    cfor(0)(_ < optimalValues.length, _ + 1) { i =>
      //println("variable" + i + ": " + optimalValues(i).formatted("%1.5f"))
      println("variable" + i + ": " + optimalValues(i))
    }
    println("Iteration number: "+opt.getIterations)
    println("Evaluation number: "+opt.getEvaluations)
  }

  def printPrediction(x: Array[Double], y: Array[Double], f: Double => Double) =
    cfor(0)(_ < x.length, _ + 1) { i =>
      //println("(" + x(i).formatted("%1.3f") + "," + y(i).formatted("%1.3f") + ") => " + f(x(i)).formatted("%1.3f"))
      println("(" + x(i) + "," + y(i) + ") => " + f(x(i)))
    }

  def fit(empiricalSemivariogram: Seq[(Double,Double)], model:ModelType): Double => Double = {
    fit(empiricalSemivariogram, model, Array.fill[Double](3)(1))
  }

  def fit(empiricalSemivariogram: Seq[(Double,Double)], model:ModelType, begin:Array[Double]): Double => Double = {
    model match {
      case Linear =>
        // Construct slope and intercept
        val regression = new SimpleRegression
        for((x,y) <- empiricalSemivariogram) { regression.addData(x,y) }
        val slope = regression.getSlope
        val intercept = regression.getIntercept
        x => slope*x + intercept

      //Least Squares minimization
      case Gaussian =>
        class GaussianProblem extends LeastSquaresFittingProblem {
          start = begin
          def valueFunc(r: Double, s: Double, a: Double): Double => Double = explicitGaussian(r, s, a)
          def jacobianFunc(variables: Array[Double]): Double => Array[Double] = jacobianGaussian(variables)
        }
        class GaussianNuggetProblem extends LeastSquaresFittingNuggetProblem {
          start = begin.drop(1)
          def valueFuncNugget(r: Double, s: Double): Double => Double = explicitGaussianNugget(r, s)
          def jacobianFuncNugget(variables: Array[Double]): Double => Array[Double] = jacobianGaussianNugget(variables)
        }

        val problem = new GaussianProblem
        for((x,y) <- empiricalSemivariogram) { problem.addPoint(x,y) }
        val opt: Optimum = optConstructor(problem)
        val optimalValues: Array[Double] = opt.getPoint.toArray
        println(empiricalSemivariogram.length)

        if (optimalValues(2) < 0) {
          val problem = new GaussianNuggetProblem
          for((x,y) <- empiricalSemivariogram) { problem.addPoint(x,y) }
          val opt: Optimum = optConstructor(problem)
          val optimalValues: Array[Double] = opt.getPoint.toArray
          printOptimization(opt)
          this.r = optimalValues(0)
          this.s = optimalValues(1)
          this.a = 0
          val definition: Double => Double = explicitGaussianNugget(optimalValues(0), optimalValues(1))
          //printPrediction(problem.x, problem.y, definition)
          definition
        }
        else {
          printOptimization(opt)
          this.r = optimalValues(0)
          this.s = optimalValues(1)
          this.a = optimalValues(2)
          val definition: Double => Double = explicitGaussian(optimalValues(0), optimalValues(1), optimalValues(2))
          //printPrediction(problem.x, problem.y, definition)
          definition
        }

      case Exponential =>
        class ExponentialProblem extends LeastSquaresFittingProblem {
          start = begin
          def valueFunc(r: Double, s: Double, a: Double): Double => Double = explicitExponential(r, s, a)
          def jacobianFunc(variables: Array[Double]): Double => Array[Double] = jacobianExponential(variables)
        }
        class ExponentialNuggetProblem extends LeastSquaresFittingNuggetProblem {
          start = begin.drop(1)
          def valueFuncNugget(r: Double, s: Double): Double => Double = explicitExponentialNugget(r, s)
          def jacobianFuncNugget(variables: Array[Double]): Double => Array[Double] = jacobianExponentialNugget(variables)
        }

        val problem = new ExponentialProblem
        for((x,y) <- empiricalSemivariogram) { problem.addPoint(x,y) }
        val opt: Optimum = optConstructor(problem)
        val optimalValues: Array[Double] = opt.getPoint.toArray

        if (optimalValues(2) < 0) {
          val problem = new ExponentialNuggetProblem
          for((x,y) <- empiricalSemivariogram) { problem.addPoint(x,y) }
          val opt: Optimum = optConstructor(problem)
          val optimalValues: Array[Double] = opt.getPoint.toArray
          printOptimization(opt)
          this.r = optimalValues(0)
          this.s = optimalValues(1)
          this.a = 0
          val definition: Double => Double = explicitExponentialNugget(optimalValues(0), optimalValues(1))
          //printPrediction(problem.x, problem.y, definition)
          definition
        }
        else {
          printOptimization(opt)
          this.r = optimalValues(0)
          this.s = optimalValues(1)
          this.a = optimalValues(2)
          val definition: Double => Double = explicitExponential(optimalValues(0), optimalValues(1), optimalValues(2))
          //printPrediction(problem.x, problem.y, definition)
          definition
        }

      case Circular =>
        class CircularProblem extends LeastSquaresFittingProblem {
          start = begin
          def valueFunc(r: Double, s: Double, a: Double): Double => Double = explicitCircular(r, s, a)
          def jacobianFunc(variables: Array[Double]): Double => Array[Double] = jacobianCircular(variables)
        }
        class CircularNuggetProblem extends LeastSquaresFittingNuggetProblem {
          start = begin.drop(1)
          def valueFuncNugget(r: Double, s: Double): Double => Double = explicitCircularNugget(r, s)
          def jacobianFuncNugget(variables: Array[Double]): Double => Array[Double] = jacobianCircularNugget(variables)
        }

        val problem = new CircularProblem
        for((x,y) <- empiricalSemivariogram) { problem.addPoint(x,y) }
        val opt: Optimum = optConstructor(problem)
        val optimalValues: Array[Double] = opt.getPoint.toArray

        if (optimalValues(2) < 0) {
          val problem = new CircularNuggetProblem
          for((x,y) <- empiricalSemivariogram) { problem.addPoint(x,y) }
          val opt: Optimum = optConstructor(problem)
          val optimalValues: Array[Double] = opt.getPoint.toArray
          printOptimization(opt)
          this.r = optimalValues(0)
          this.s = optimalValues(1)
          this.a = 0
          val definition: Double => Double = explicitCircularNugget(optimalValues(0), optimalValues(1))
          //printPrediction(problem.x, problem.y, definition)
          definition
        }
        else {
          printOptimization(opt)
          this.r = optimalValues(0)
          this.s = optimalValues(1)
          this.a = optimalValues(2)
          val definition: Double => Double = explicitCircular(optimalValues(0), optimalValues(1), optimalValues(2))
          //printPrediction(problem.x, problem.y, definition)
          definition
        }

      case Spherical =>
        class SphericalProblem extends LeastSquaresFittingProblem {
          start = begin
          def valueFunc(r: Double, s: Double, a: Double): Double => Double = explicitSpherical(r, s, a)
          def jacobianFunc(variables: Array[Double]): Double => Array[Double] = jacobianSpherical(variables)
        }
        class SphericalNuggetProblem extends LeastSquaresFittingNuggetProblem {
          start = begin.drop(1)
          def valueFuncNugget(w: Double, s: Double): Double => Double = explicitSphericalNugget(w, s)
          def jacobianFuncNugget(variables: Array[Double]): Double => Array[Double] = jacobianSphericalNugget(variables)
        }

        val problem = new SphericalProblem
        for((x,y) <- empiricalSemivariogram) { problem.addPoint(x,y) }
        val opt: Optimum = optConstructor(problem)
        val optimalValues: Array[Double] = opt.getPoint.toArray

        if (optimalValues(2) < 0) {
          val problem = new SphericalNuggetProblem
          for((x,y) <- empiricalSemivariogram) { problem.addPoint(x,y) }
          val opt: Optimum = optConstructor(problem)
          val optimalValues: Array[Double] = opt.getPoint.toArray
          printOptimization(opt)
          this.r = optimalValues(0)
          this.s = optimalValues(1)
          this.a = 0
          val definition: Double => Double = explicitSphericalNugget(optimalValues(0), optimalValues(1))
          //printPrediction(problem.x, problem.y, definition)
          definition
        }
        else {
          printOptimization(opt)
          this.r = optimalValues(0)
          this.s = optimalValues(1)
          this.a = optimalValues(2)
          val definition: Double => Double = explicitSpherical(optimalValues(0), optimalValues(1), optimalValues(2))
          //printPrediction(problem.x, problem.y, definition)
          definition
        }

      case Wave =>
        class WaveProblem extends LeastSquaresFittingProblem {
          start = begin
          def valueFunc(w: Double, s: Double, a: Double): Double => Double = explicitWave(w, s, a)
          def jacobianFunc(variables: Array[Double]): Double => Array[Double] = jacobianWave(variables)
        }
        class WaveNuggetProblem extends LeastSquaresFittingNuggetProblem {
          start = begin.drop(1)
          def valueFuncNugget(w: Double, s: Double): Double => Double = explicitWaveNugget(w, s)
          def jacobianFuncNugget(variables: Array[Double]): Double => Array[Double] = jacobianWaveNugget(variables)
        }

        val problem = new WaveProblem
        for((x,y) <- empiricalSemivariogram) { problem.addPoint(x,y) }
        val opt: Optimum = optConstructor(problem)
        val optimalValues: Array[Double] = opt.getPoint.toArray

        if (optimalValues(2) < 0) {
          val problem = new WaveNuggetProblem
          for((x,y) <- empiricalSemivariogram) { problem.addPoint(x,y) }
          val opt: Optimum = optConstructor(problem)
          val optimalValues: Array[Double] = opt.getPoint.toArray
          printOptimization(opt)
          this.r = optimalValues(0)
          this.s = optimalValues(1)
          this.a = 0
          val definition: Double => Double = explicitWaveNugget(optimalValues(0), optimalValues(1))
          //printPrediction(problem.x, problem.y, definition)
          definition
        }
        else {
          printOptimization(opt)
          this.r = optimalValues(0)
          this.s = optimalValues(1)
          this.a = optimalValues(2)
          val definition: Double => Double = explicitWave(optimalValues(0), optimalValues(1), optimalValues(2))
          //printPrediction(problem.x, problem.y, definition)
          definition
        }
    }
  }

  def constructEmpirical(pts:Seq[PointFeature[Double]], radius:Option[Double]=None, lag:Double=0, model:ModelType): Seq[(Double,Double)] = {
    def distance(p1: Point, p2: Point) = math.abs(math.sqrt(math.pow(p1.x - p2.x,2) + math.pow(p1.y - p2.y,2)))

    // every pair of points and their distance from each other
    val distancePairs:Seq[(Double,(PointFeature[Double],PointFeature[Double]))] = {
      //println("radius=" + radius)
      radius match {
        case Some(dmax) =>
          //println("Some(dmax)" + dmax)
          makePairs(pts.toList)
            .map { case (a, b) => (distance(a.geom, b.geom), (a, b)) }
            .filter { case (distance, _) => distance <= dmax }
            .toSeq
        case None =>
          makePairs(pts.toList)
            .map { case (a, b) => (distance(a.geom, b.geom), (a, b)) }
            .toSeq
      }
    }

    val buckets:Seq[Bucket] =
      if(lag == 0) {
        println()
        val abc =
          distancePairs
            .map{ case(d,_) => d }
            .distinct
            .map { d => Bucket(d,d) }
        println("Buckets : ")
        println(abc.mkString("\n"))
        abc
      } else {
        println()
        println("Distance Pairs : ")
        println(distancePairs.mkString(" "))
        // the maximum distance between two points in the field
        val dmax: Double = distancePairs.map{ case(d,_) => d }.max
        println("dmax = " + dmax)
        // the lower limit of the largest bucket
        val E = 1e-4
        val lowerLimit: Double = model match {
          case Linear => (Math.floor(dmax/lag).toInt * lag) + 1
          case _      => dmax + E
        }
        println("lowerLimit = " + lowerLimit)

        ((0.0 to lowerLimit by lag) toList).zip((lag to (lowerLimit + lag) by lag) toList)
          .map{ case(start,end) => Bucket(start,end) }
      }
    println("lag = " + lag)
    //println(buckets.mkString("\n"))

    // populate the buckets
    for( (d,(x,y)) <- distancePairs ) {
      //println("(" + d + "," + "(" + x + ", " + y + ")")
      buckets.find(b => b.contains(d)) match {
        case Some(b) => b.add(x,y)
        case None => sys.error(s"Points $x and $y don't fit any bucket")
      }
    }

    // use midpoint of buckets for distance
    val empiricalSemivariogram:Seq[(Double,Double)] =
    // empty buckets are first removed
      buckets.filter ( b => !b.isEmpty)
        .map { b => (b.midpoint,b.semivariance) }

    empiricalSemivariogram
  }

  def apply(pts:Seq[PointFeature[Double]],radius:Option[Double]=None,lag:Double=0,model:ModelType):Double => Double = {

    //Constructing Empirical Semivariogram
    val empiricalSemivariogram:Seq[(Double,Double)] = constructEmpirical(pts, radius, lag, model)

    //Fitting the empirical variogram to the input model
    fit(empiricalSemivariogram, model)
  }
}