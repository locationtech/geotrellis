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

abstract class Semivariogram(val range: Double,
                             val sill: Double,
                             val nugget: Double) {
  def apply(x: Double): Double
}

object Semivariogram {
  def apply(f: Double => Double, range: Double, sill: Double, nugget: Double): Semivariogram =
    new Semivariogram(range, sill, nugget) {
      def apply(x: Double): Double = f(x)
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
  def fit(empiricalSemivariogram: EmpiricalVariogram,
          model: ModelType,
          begin: Array[Double]): Semivariogram = {
    model match {
      //Least Squares minimization
      case Gaussian =>
        //Gaussian Problem
        val problem =
          new LeastSquaresFittingProblem(empiricalSemivariogram.distances, empiricalSemivariogram.variance, begin) {
            override def valueFunc(r: Double, s: Double, a: Double): (Double) => Double =
              NonLinearSemivariogram.explicitModel(r, s, a, Gaussian)
            override def jacobianFunc(variables: Array[Double]): (Double) => Array[Double] =
              NonLinearSemivariogram.jacobianModel(variables, Gaussian)
          }
        val optimalValues: Array[Double] = problem.optimum.getPoint.toArray

        if (optimalValues(2) < 0) {
          //Gaussian Nugget Problem
          val nuggetProblem =
            new LeastSquaresFittingNuggetProblem(empiricalSemivariogram.distances, empiricalSemivariogram.variance, begin.dropRight(1)) {
              override def valueFuncNugget(r: Double, s: Double): (Double) => Double =
                NonLinearSemivariogram.explicitNuggetModel(r, s, Gaussian)
              override def jacobianFuncNugget(variables: Array[Double]): (Double) => Array[Double] =
                NonLinearSemivariogram.jacobianModel(variables, Gaussian)
            }

          val optimalValues: Array[Double] = nuggetProblem.optimum.getPoint.toArray
          NonLinearSemivariogram(optimalValues(0), optimalValues(1), Gaussian)
        }
        else
          NonLinearSemivariogram(optimalValues(0), optimalValues(1), optimalValues(2), Gaussian)

      case Exponential =>
        //Exponential Problem
        val problem =
          new LeastSquaresFittingProblem(empiricalSemivariogram.distances, empiricalSemivariogram.variance, begin) {
            override def valueFunc(r: Double, s: Double, a: Double): (Double) => Double =
              NonLinearSemivariogram.explicitModel(r, s, a, Exponential)
            override def jacobianFunc(variables: Array[Double]): (Double) => Array[Double] =
              NonLinearSemivariogram.jacobianModel(variables, Exponential)
          }
        val optimalValues: Array[Double] = problem.optimum.getPoint.toArray

        if (optimalValues(2) < 0) {
          val nuggetProblem =
            new LeastSquaresFittingNuggetProblem(empiricalSemivariogram.distances, empiricalSemivariogram.variance, begin.dropRight(1)) {
              override def valueFuncNugget(r: Double, s: Double): (Double) => Double =
                NonLinearSemivariogram.explicitNuggetModel(r, s, Exponential)
              override def jacobianFuncNugget(variables: Array[Double]): (Double) => Array[Double] =
                NonLinearSemivariogram.jacobianModel(variables, Exponential)
            }

          val optimalValues: Array[Double] = nuggetProblem.optimum.getPoint.toArray
          NonLinearSemivariogram(optimalValues(0), optimalValues(1), Exponential)
        }
        else
          NonLinearSemivariogram(optimalValues(0), optimalValues(1), optimalValues(2), Exponential)

      case Circular =>
        //Circular Problem
        val problem =
          new LeastSquaresFittingProblem(empiricalSemivariogram.distances, empiricalSemivariogram.variance, begin) {
            override def valueFunc(r: Double, s: Double, a: Double): (Double) => Double =
              NonLinearSemivariogram.explicitModel(r, s, a, Circular)
            override def jacobianFunc(variables: Array[Double]): (Double) => Array[Double] =
              NonLinearSemivariogram.jacobianModel(variables, Circular)
          }
        val optimalValues: Array[Double] = problem.optimum.getPoint.toArray

        if (optimalValues(2) < 0) {
          //Circular Nugget Problem
          val nuggetProblem =
            new LeastSquaresFittingNuggetProblem(empiricalSemivariogram.distances, empiricalSemivariogram.variance, begin.dropRight(1)) {
              override def valueFuncNugget(r: Double, s: Double): (Double) => Double =
                NonLinearSemivariogram.explicitNuggetModel(r, s, Circular)
              override def jacobianFuncNugget(variables: Array[Double]): (Double) => Array[Double] =
                NonLinearSemivariogram.jacobianModel(variables, Circular)
            }

          val optimalValues: Array[Double] = nuggetProblem.optimum.getPoint.toArray
          NonLinearSemivariogram(optimalValues(0), optimalValues(1), Circular)
        }
        else
          NonLinearSemivariogram(optimalValues(0), optimalValues(1), optimalValues(2), Circular)

      case Spherical =>
        //Spherical Problem
        val problem =
          new LeastSquaresFittingProblem(empiricalSemivariogram.distances, empiricalSemivariogram.variance, begin) {
            override def valueFunc(r: Double, s: Double, a: Double): (Double) => Double =
              NonLinearSemivariogram.explicitModel(r, s, a, Spherical)
            override def jacobianFunc(variables: Array[Double]): (Double) => Array[Double] =
              NonLinearSemivariogram.jacobianModel(variables, Spherical)
          }
        val optimalValues: Array[Double] = problem.optimum.getPoint.toArray

        if (optimalValues(2) < 0) {
          //Spherical Nugget Problem
          val nuggetProblem =
            new LeastSquaresFittingNuggetProblem(empiricalSemivariogram.distances, empiricalSemivariogram.variance, begin.dropRight(1)) {
              override def valueFuncNugget(r: Double, s: Double): (Double) => Double =
                NonLinearSemivariogram.explicitNuggetModel(r, s, Spherical)
              override def jacobianFuncNugget(variables: Array[Double]): (Double) => Array[Double] =
                NonLinearSemivariogram.jacobianModel(variables, Spherical)
            }

          val optimalValues: Array[Double] = nuggetProblem.optimum.getPoint.toArray
          NonLinearSemivariogram(optimalValues(0), optimalValues(1), Spherical)
        }
        else
          NonLinearSemivariogram(optimalValues(0), optimalValues(1), optimalValues(2), Spherical)

      case Wave =>
        //Wave Problem
        val problem =
          new LeastSquaresFittingProblem(empiricalSemivariogram.distances, empiricalSemivariogram.variance, begin) {
            override def valueFunc(w: Double, s: Double, a: Double): (Double) => Double =
              NonLinearSemivariogram.explicitModel(w, s, a, Wave)
            override def jacobianFunc(variables: Array[Double]): (Double) => Array[Double] =
              NonLinearSemivariogram.jacobianModel(variables, Wave)
          }
        val optimalValues: Array[Double] = problem.optimum.getPoint.toArray

        if (optimalValues(2) < 0) {
          //Wave Nugget Problem
          val nuggetProblem =
            new LeastSquaresFittingNuggetProblem(empiricalSemivariogram.distances, empiricalSemivariogram.variance, begin.dropRight(1)) {
              override def valueFuncNugget(w: Double, s: Double): (Double) => Double =
                NonLinearSemivariogram.explicitNuggetModel(w, s, Wave)
              override def jacobianFuncNugget(variables: Array[Double]): (Double) => Array[Double] =
                NonLinearSemivariogram.jacobianModel(variables, Wave)
            }

          val optimalValues: Array[Double] = nuggetProblem.optimum.getPoint.toArray
          NonLinearSemivariogram(optimalValues(0), optimalValues(1), Wave)
        }
        else
          NonLinearSemivariogram(optimalValues(0), optimalValues(1), optimalValues(2), Wave)

      case _ => throw new UnsupportedOperationException("Fitting for $model can not be performed in this function")
    }
  }
}
