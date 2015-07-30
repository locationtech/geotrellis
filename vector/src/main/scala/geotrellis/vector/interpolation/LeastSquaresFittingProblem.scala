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
import spire.syntax.cfor._

/**
 * @author Vishal Anand
 */

/**
 * Computes fitting of the given empirical semivariogram using a [[ModelType]]'s function definitions valueFunc() and jacobianFunc()
 * @param x     Empirical Semivariogram distance value
 * @param y     Empirical Semivariogram's corresponding variance values
 * @param start Starting point for finding the optimization values of Semivariogram's parameters (range, sill, 0)
 */
abstract class LeastSquaresFittingProblem(x: Array[Double], y: Array[Double], start: Array[Double]) {
  /**
   * @param r Denotes current Range of [[Semivariogram]] while performing fitting optimization
   * @param s Denotes current Sill of [[Semivariogram]] while performing fitting optimization
   */
  def valueFunc(r: Double, s: Double, a: Double): Double => Double

  /**
   * Computes the differential values at the current point of Levenberg-Marquard optimization
   */
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

  //Actual method which performs the optimizations
  def optimum: Optimum = {
    val lsb: LeastSquaresBuilder = new LeastSquaresBuilder()
    val lmo: LevenbergMarquardtOptimizer = new LevenbergMarquardtOptimizer()

    lsb.model(retMVF(), retMMF())
    lsb.target(y)
    lsb.start(start)
    lsb.maxEvaluations(Int.MaxValue)
    lsb.maxIterations(Int.MaxValue)

    val lsp: LeastSquaresProblem = lsb.build
    lmo.optimize(lsp)
  }
}
