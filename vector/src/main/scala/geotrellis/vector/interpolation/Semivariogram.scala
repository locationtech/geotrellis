/*
 * Copyright (c) 2015 Azavea.
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

package geotrellis.vector.interpolation

import org.apache.commons.math3.analysis.{MultivariateMatrixFunction, MultivariateVectorFunction}
import org.apache.commons.math3.fitting.leastsquares.LeastSquaresOptimizer.Optimum
import org.apache.commons.math3.fitting.leastsquares.{LeastSquaresBuilder, LeastSquaresProblem, LevenbergMarquardtOptimizer}
import org.apache.commons.math3.stat.regression.SimpleRegression
import spire.syntax.cfor._

/**
 * @author Vishal Anand
 */

abstract sealed class ModelType

case class Linear (radius: Option[Double], lag: Double) extends ModelType

object Linear {
  def apply(): Linear =
    Linear(None, 0.0)

  def withLag(lag: Double): Linear =
    Linear(None, lag)

  def withRadius(radius: Double): Linear =
    Linear(Option(radius), 0.0)

  def apply(radius: Double, lag: Double): Linear =
    Linear(Some(radius), lag)
}

abstract class NonLinearModelType(maxDist: Double, binMax: Int)

// Non-linear
case object Gaussian extends ModelType
case object Circular extends ModelType
case object Spherical extends ModelType
case object Exponential extends ModelType
case object Wave extends ModelType

abstract class Semivariogram(val range: Double, val sill: Double, val nugget: Double) {
  def apply(x: Double): Double
}

object Semivariogram {
  def apply(f: Double => Double, range: Double, sill: Double, nugget: Double): Semivariogram =
    new Semivariogram(range, sill, nugget) {
      def apply(x: Double): Double = f(x)
    }

  var r: Double = 0   //Range
  var s: Double = 0   //Sill
  var a: Double = 0   //Nugget

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
      println("variable" + i + ": " + optimalValues(i))
    }
    println("Iteration number: "+opt.getIterations)
    println("Evaluation number: "+opt.getEvaluations)
  }

  def printPrediction(x: Array[Double], y: Array[Double], f: Double => Double) =
    cfor(0)(_ < x.length, _ + 1) { i =>
      println("(" + x(i) + "," + y(i) + ") => " + f(x(i)))
    }

  /**
   * @param empiricalSemivariogram  is the input which has to be fitted into a Semivariogram model
   * @param model                   the [[ModelType]] into which the input has to be fitted
   * @return                        [[Semivariogram]]
   */
  def fit(empiricalSemivariogram: EmpiricalVariogram, model: ModelType): Semivariogram = {
    fit(empiricalSemivariogram, model, Array.fill[Double](3)(1))
  }

  /**
   * @param empiricalSemivariogram  is the input which has to be fitted into a Semivariogram model
   * @param model                   the [[ModelType]] into which the input has to be fitted
   * @param begin                   the starting point of the optimization search of (range, sill, nugget) values
   * @return                        [[Semivariogram]]
   */
  def fit(empiricalSemivariogram: EmpiricalVariogram, model: ModelType, begin: Array[Double]): Semivariogram = {
    model match {
      //Least Squares minimization
      case Gaussian =>
        class GaussianProblem extends LeastSquaresFittingProblem {
          start = begin
          def valueFunc(r: Double, s: Double, a: Double): Double => Double = NonLinearSemivariogram.explicitModel(r, s, a, Gaussian)
          def jacobianFunc(variables: Array[Double]): Double => Array[Double] = NonLinearSemivariogram.jacobianModel(variables, Gaussian)
        }
        class GaussianNuggetProblem extends LeastSquaresFittingNuggetProblem {
          start = begin.dropRight(1)
          def valueFuncNugget(r: Double, s: Double): Double => Double = NonLinearSemivariogram.explicitNuggetModel(r, s, Gaussian)
          def jacobianFuncNugget(variables: Array[Double]): Double => Array[Double] =  NonLinearSemivariogram.jacobianModel(variables, Gaussian)
        }

        val problem = new GaussianProblem
        problem.x = empiricalSemivariogram.distances
        problem.y = empiricalSemivariogram.variance
        val opt: Optimum = optConstructor(problem)
        val optimalValues: Array[Double] = opt.getPoint.toArray

        if (optimalValues(2) < 0) {
          val problem = new GaussianNuggetProblem
          problem.x = empiricalSemivariogram.distances
          problem.y = empiricalSemivariogram.variance
          val opt: Optimum = optConstructor(problem)
          val optimalValues: Array[Double] = opt.getPoint.toArray
          this.r = optimalValues(0)
          this.s = optimalValues(1)
          this.a = 0
          NonLinearSemivariogram(optimalValues(0), optimalValues(1), Gaussian)
        }
        else {
          this.r = optimalValues(0)
          this.s = optimalValues(1)
          this.a = optimalValues(2)
          NonLinearSemivariogram(optimalValues(0), optimalValues(1), optimalValues(2), Gaussian)
        }

      case Exponential =>
        class ExponentialProblem extends LeastSquaresFittingProblem {
          start = begin
          def valueFunc(r: Double, s: Double, a: Double): Double => Double = NonLinearSemivariogram.explicitModel(r, s, a, Exponential)
          def jacobianFunc(variables: Array[Double]): Double => Array[Double] = NonLinearSemivariogram.jacobianModel(variables, Exponential)
        }
        class ExponentialNuggetProblem extends LeastSquaresFittingNuggetProblem {
          start = begin.dropRight(1)
          def valueFuncNugget(r: Double, s: Double): Double => Double = NonLinearSemivariogram.explicitNuggetModel(r, s, Exponential)
          def jacobianFuncNugget(variables: Array[Double]): Double => Array[Double] = NonLinearSemivariogram.jacobianModel(variables, Exponential)
        }

        val problem = new ExponentialProblem
        problem.x = empiricalSemivariogram.distances
        problem.y = empiricalSemivariogram.variance
        val opt: Optimum = optConstructor(problem)
        val optimalValues: Array[Double] = opt.getPoint.toArray

        if (optimalValues(2) < 0) {
          val problem = new ExponentialNuggetProblem
          problem.x = empiricalSemivariogram.distances
          problem.y = empiricalSemivariogram.variance
          val opt: Optimum = optConstructor(problem)
          val optimalValues: Array[Double] = opt.getPoint.toArray
          this.r = optimalValues(0)
          this.s = optimalValues(1)
          this.a = 0
          NonLinearSemivariogram(optimalValues(0), optimalValues(1), Exponential)
        }
        else {
          this.r = optimalValues(0)
          this.s = optimalValues(1)
          this.a = optimalValues(2)
          NonLinearSemivariogram(optimalValues(0), optimalValues(1), optimalValues(2), Exponential)
        }

      case Circular =>
        class CircularProblem extends LeastSquaresFittingProblem {
          start = begin
          def valueFunc(r: Double, s: Double, a: Double): Double => Double = NonLinearSemivariogram.explicitModel(r, s, a, Circular)
          def jacobianFunc(variables: Array[Double]): Double => Array[Double] = NonLinearSemivariogram.jacobianModel(variables, Circular)
        }
        class CircularNuggetProblem extends LeastSquaresFittingNuggetProblem {
          start = begin.dropRight(1)
          def valueFuncNugget(r: Double, s: Double): Double => Double = NonLinearSemivariogram.explicitNuggetModel(r, s, Circular)
          def jacobianFuncNugget(variables: Array[Double]): Double => Array[Double] = NonLinearSemivariogram.jacobianModel(variables, Circular)
        }

        val problem = new CircularProblem
        problem.x = empiricalSemivariogram.distances
        problem.y = empiricalSemivariogram.variance
        val opt: Optimum = optConstructor(problem)
        val optimalValues: Array[Double] = opt.getPoint.toArray

        if (optimalValues(2) < 0) {
          val problem = new CircularNuggetProblem
          problem.x = empiricalSemivariogram.distances
          problem.y = empiricalSemivariogram.variance
          val opt: Optimum = optConstructor(problem)
          val optimalValues: Array[Double] = opt.getPoint.toArray
          this.r = optimalValues(0)
          this.s = optimalValues(1)
          this.a = 0
          NonLinearSemivariogram(optimalValues(0), optimalValues(1), Circular)
        }
        else {
          this.r = optimalValues(0)
          this.s = optimalValues(1)
          this.a = optimalValues(2)
          NonLinearSemivariogram(optimalValues(0), optimalValues(1), optimalValues(2), Circular)
        }

      case Spherical =>
        class SphericalProblem extends LeastSquaresFittingProblem {
          start = begin
          def valueFunc(r: Double, s: Double, a: Double): Double => Double = NonLinearSemivariogram.explicitModel(r, s, a, Spherical)
          def jacobianFunc(variables: Array[Double]): Double => Array[Double] = NonLinearSemivariogram.jacobianModel(variables, Spherical)
        }
        class SphericalNuggetProblem extends LeastSquaresFittingNuggetProblem {
          start = begin.dropRight(1)
          def valueFuncNugget(r: Double, s: Double): Double => Double = NonLinearSemivariogram.explicitNuggetModel(r, s, Spherical)
          def jacobianFuncNugget(variables: Array[Double]): Double => Array[Double] = NonLinearSemivariogram.jacobianModel(variables, Spherical)
        }

        val problem = new SphericalProblem
        problem.x = empiricalSemivariogram.distances
        problem.y = empiricalSemivariogram.variance
        val opt: Optimum = optConstructor(problem)
        val optimalValues: Array[Double] = opt.getPoint.toArray

        if (optimalValues(2) < 0) {
          val problem = new SphericalNuggetProblem
          problem.x = empiricalSemivariogram.distances
          problem.y = empiricalSemivariogram.variance
          val opt: Optimum = optConstructor(problem)
          val optimalValues: Array[Double] = opt.getPoint.toArray
          this.r = optimalValues(0)
          this.s = optimalValues(1)
          this.a = 0
          NonLinearSemivariogram(optimalValues(0), optimalValues(1), Spherical)
        }
        else {
          this.r = optimalValues(0)
          this.s = optimalValues(1)
          this.a = optimalValues(2)
          NonLinearSemivariogram(optimalValues(0), optimalValues(1), optimalValues(2), Spherical)
        }

      case Wave =>
        class WaveProblem extends LeastSquaresFittingProblem {
          start = begin
          def valueFunc(w: Double, s: Double, a: Double): Double => Double = NonLinearSemivariogram.explicitModel(w, s, a, Wave)
          def jacobianFunc(variables: Array[Double]): Double => Array[Double] = NonLinearSemivariogram.jacobianModel(variables, Wave)
        }
        class WaveNuggetProblem extends LeastSquaresFittingNuggetProblem {
          start = begin.dropRight(1)
          def valueFuncNugget(w: Double, s: Double): Double => Double = NonLinearSemivariogram.explicitNuggetModel(w, s, Wave)
          def jacobianFuncNugget(variables: Array[Double]): Double => Array[Double] = NonLinearSemivariogram.jacobianModel(variables, Wave)
        }

        val problem = new WaveProblem
        problem.x = empiricalSemivariogram.distances
        problem.y = empiricalSemivariogram.variance
        val opt: Optimum = optConstructor(problem)
        val optimalValues: Array[Double] = opt.getPoint.toArray

        if (optimalValues(2) < 0) {
          val problem = new WaveNuggetProblem
          problem.x = empiricalSemivariogram.distances
          problem.y = empiricalSemivariogram.variance
          val opt: Optimum = optConstructor(problem)
          val optimalValues: Array[Double] = opt.getPoint.toArray
          this.r = optimalValues(0)
          this.s = optimalValues(1)
          this.a = 0
          NonLinearSemivariogram(optimalValues(0), optimalValues(1), Wave)
        }
        else {
          this.r = optimalValues(0)
          this.s = optimalValues(1)
          this.a = optimalValues(2)
          NonLinearSemivariogram(optimalValues(0), optimalValues(1), optimalValues(2), Wave)
        }
      case _ => throw new UnsupportedOperationException("Fitting for $model can not be done in this function")
    }
  }
}
