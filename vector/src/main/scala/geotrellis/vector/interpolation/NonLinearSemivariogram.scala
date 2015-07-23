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

import geotrellis.vector._

object NonLinearSemivariogram {

  def explicitGaussian(r: Double, s: Double, a: Double): Double => Double = {
    h: Double => {
      if (h == 0) 0
      else
        a + (s - a) * (1 - math.exp(- math.pow(h, 2) / math.pow(r, 2)))
    }
    /*                      | 0                                 . h = 0
     *  gamma(h; r, s, a) = |
     *                      | a + (s - a) {1 - e^(-h^2 / r^2)}  , h > 0
     */
  }

  def explicitGaussianNugget(r: Double, s: Double): Double => Double = {
    h: Double => {
      if (h == 0) 0
      else
        s * (1 - math.exp(- math.pow(h, 2) / math.pow(r, 2)))
    }
    /*                   | 0                             . h = 0
     *  gamma(h; r, s) = |
     *                   | s {1 - e^(-h^2 / r^2)}          , h > 0
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
    /*                      | 0                                                                        , h = 0
     *                      |
     *                      |               |                                              _________ |
     *                      |               |      2                | h |      2h         /    h^2   |
     *  gamme(h; r, s, a) = | a + (s - a) * |1 - ----- * cos_inverse|---| + -------- *   /1 - -----  | , 0 < h <= r
     *                      |               |      pi               | r |    pi * r    \/      r^2   |
     *                      |               |                                                        |
     *                      |
     *                      | s                                                                        , h > r
     */
  }

  def explicitCircularNugget(r: Double, s: Double): Double => Double = {
    h: Double => {
      if (h == 0) 0
      else if (h > r)
        s
      else
        s * (1 - (2 / math.Pi) * math.acos(h/r) + ((2 * h) / (math.Pi * r)) * math.sqrt(1 - math.pow(h, 2) / math.pow(r, 2)))
    }

    /*                   | 0                                                              , h = 0
     *                   |
     *                   |     |                                              _________ |
     *                   |     |      2                | h |      2h         /    h^2   |
     *  gamme(h; r, s) = | s * |1 - ----- * cos_inverse|---| + -------- *   /1 - -----  | , 0 < h <= r
     *                   |     |      pi               | r |    pi * r    \/      r^2   |
     *                   |     |                                                        |
     *                   |
     *                   | s                                                              , h > r
     */
  }

  def jacobianCircular(variables: Array[Double]): Double => Array[Double] = {
    var jacobianRet: Array[Double] = Array.ofDim[Double](3)
    x: Double => {
      if (x == 0)
        jacobianRet = Array.fill[Double](3)(0)
      else {
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
        jacobianRet(0) = -4 * x * variables(1) * math.sqrt(math.pow(variables(0), 2) - math.pow(x, 2)) / (math.Pi * math.pow(variables(0), 3))
        jacobianRet(1) = 1 - (2 / math.Pi) * math.acos(x / variables(0)) + ((2 * x) / (math.Pi * variables(0))) * math.sqrt(1 - math.pow(x / variables(0), 2))
      }
      jacobianRet
    }
  }

  def explicitSpherical(r: Double, s: Double, a: Double): Double => Double = {
    h: Double => {
      //if (h == 0) 0
      //TODO : Investigate if the standard usage is f(0) = a or f(0) = 0
      if (h == 0) a
      else if (h > r) s
      else
        a + (s - a) * ((3 * h / (2 * r)) - (math.pow(h, 3) / (2 * math.pow(r, 3)) ))
    }
    /*                      | 0                             . h = 0
     *                      |             | 3h      h^3   |
     *  gamma(h; r, s, a) = | a + (s - a) |---- - ------- | , 0 < h <= r
     *                      |             | 2r     2r^3   |
     *                      | s                             , h > r
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
    /*                   | 0                    . h = 0
     *                   |    | 3h      h^3   |
     *  gamma(h; r, s) = | s  |---- - ------- | , 0 < h <= r
     *                   |    | 2r     2r^3   |
     *                   | s                    , h > r
     */
  }

  def jacobianSpherical(variables: Array[Double]): Double => Array[Double] = {
    var jacobianRet: Array[Double] = Array.ofDim[Double](3)
    x: Double => {
      if (x == 0)
        jacobianRet = Array.fill[Double](3)(0)
      else if (x > 0 && x <= variables(0)) {
        jacobianRet(0) = (variables(1) - variables(2)) * ((-3 * x)/(2 * math.pow(variables(0), 2)) + (3 * math.pow(x, 3)/(2 * math.pow(variables(0), 4))))
        jacobianRet(1) = ((3 * x)/(2 * variables(0))) - (0.5 * math.pow(x / variables(0), 3))
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
        jacobianRet(0) = variables(1) * ((-3 * x) / (2 * math.pow(variables(0), 2)) + (3 * math.pow(x, 3)/(2 * math.pow(variables(0), 4))))
        jacobianRet(1) = ((3 * x)/(2 * variables(0))) - (0.5 * math.pow(x / variables(0), 3))
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
    /*                      | 0                                  . h = 0
     *  gamma(h; r, s, a) = |
     *                      | a + (s - a) {1 - e^(-3 * h / r)}   , h > 0
     */
  }

  def explicitExponentialNugget(r: Double, s: Double): Double => Double = {
    h: Double => {
      if (h == 0) 0
      else
        s * (1 - math.exp(- 3 * h / r))
    }
    /*                   | 0                        . h = 0
     *  gamma(h; r, s) = |
     *                   | s {1 - e^(-3 * h / r)}   , h > 0
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
    /*                      | 0                                 . h = 0
     *                      |
     *  gamma(h; w, s, a) = |             |       sin(h / w)  |
     *                      | a + (s - a) |1 - w ------------ | , h > 0
     *                      |             |           h       |
     */
  }

  def explicitWaveNugget(w: Double, s: Double): Double => Double = {
    h: Double => {
      if (h == 0) 0
      else
        s * (1 - w * math.sin(h / w) / h)
    }
    /*                   | 0                        . h = 0
     *                   |
     *  gamma(h; w, s) = |    |       sin(h / w)  |
     *                   | s  |1 - w ------------ | , h > 0
     *                   |    |           h       |
     */
  }

  def jacobianWave(variables: Array[Double]): Double => Array[Double] = {
    var jacobianRet: Array[Double] = Array.ofDim[Double](3)
    x: Double => {
      if (x == 0)
        jacobianRet = Array.fill[Double](3)(0)
      else {
        jacobianRet(0) = (variables(1) - variables(2)) * ((math.cos(x / variables(0)) / variables(0)) - (math.sin(x / variables(0)) / x))
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
        jacobianRet(0) = variables(1) * ((math.cos(x / variables(0)) / variables(0)) - (math.sin(x / variables(0)) / x))
        jacobianRet(1) = 1 - variables(0) * math.sin(x / variables(0)) / x
      }
      jacobianRet
    }
  }

  def explicitNuggetModel(svParam: Array[Double], model: ModelType): Double => Double = {
    val (range: Double, sill: Double) = (svParam(0), svParam(1))
    explicitNuggetModel(range, sill, model)
  }
  def explicitNuggetModel(range: Double, sill: Double, model: ModelType): Double => Double = {
    model match {
      case Circular     =>  explicitCircularNugget(range, sill)
      case Spherical    =>  explicitSphericalNugget(range, sill)
      case Gaussian     =>  explicitGaussianNugget(range, sill)
      case Exponential  =>  explicitExponentialNugget(range, sill)
      case Wave         =>  explicitWaveNugget(range, sill)
      case _            => throw new UnsupportedOperationException("$model is an invalid model or is not implemented")
    }
  }

  def explicitModel(svParam: Array[Double], model: ModelType): Double => Double = {
    val (range: Double, sill: Double, nugget: Double) = (svParam(0), svParam(1), svParam(2))
    explicitModel(range, sill, nugget, model)
  }
  def explicitModel(range: Double, sill: Double, nugget: Double, model: ModelType): Double => Double = {
    model match {
      case Circular     =>  explicitCircular(range, sill, nugget)
      case Spherical    =>  explicitSpherical(range, sill, nugget)
      case Gaussian     =>  explicitGaussian(range, sill, nugget)
      case Exponential  =>  explicitExponential(range, sill, nugget)
      case Wave         =>  explicitWave(range, sill, nugget)
      case _            => throw new UnsupportedOperationException("$model is an invalid model or is not implemented")
    }
  }

  def jacobianModel(variables: Array[Double], model: ModelType): Double => Array[Double] = {
    if(variables.length == 3)
      model match {
        case Circular     => jacobianCircular(variables)
        case Spherical    => jacobianSpherical(variables)
        case Gaussian     => jacobianGaussian(variables)
        case Exponential  => jacobianExponential(variables)
        case Wave         => jacobianWave(variables)
        case _            => throw new UnsupportedOperationException("$model is an invalid model or is not implemented")
      }
    else
      model match {
        case Circular     => jacobianCircularNugget(variables)
        case Spherical    => jacobianSphericalNugget(variables)
        case Gaussian     => jacobianGaussianNugget(variables)
        case Exponential  => jacobianExponentialNugget(variables)
        case Wave         => jacobianWaveNugget(variables)
        case _            => throw new UnsupportedOperationException("$model is an invalid model or is not implemented")
      }
  }

  def apply(svParam: Array[Double], model: ModelType): Semivariogram = {
    if(svParam.length == 3)
      Semivariogram(explicitModel(svParam, model), svParam(0), svParam(1), svParam(2))
    else
      Semivariogram(explicitNuggetModel(svParam, model), svParam(0), svParam(1), 0)
  }

  def apply(range: Double, sill: Double, nugget: Double, model: ModelType): Semivariogram = {
    Semivariogram(explicitModel(range, sill, nugget, model), range, sill, nugget)
  }

  def apply(range: Double, sill: Double, model: ModelType): Semivariogram = {
    Semivariogram(explicitNuggetModel(range, sill, model), range, sill, 0)
  }

  def apply(pts: Array[PointFeature[Double]], maxdist: Double, binmax: Int, model: ModelType): Semivariogram = {
    // Construct slope and intercept
    val abc = EmpiricalVariogram.nonlinear(pts, maxdist, binmax)
    val empiricalSemivariogram: Array[(Double, Double)] = Array.tabulate(abc.distances.length){i => (abc.distances(i), abc.variance(i))}
    val es = empiricalSemivariogram

    //Fitting the empirical variogram to the input model
    def stdev(data: Array[Double]): Double = {
      if (data.length < 2)
        return Double.NaN
      // average
      val mean: Double = data.sum / data.length
      // reduce function
      def f(sum: Double, tail: Double): Double = {
        val dif = tail - mean
        sum + dif * dif
      }

      val sum = data.foldLeft(0.0)((s, t) => f(s, t))
      math.sqrt(sum / (data.length - 1))
    }
    val D: Array[Double] = Array.tabulate(es.length){i => es(i)._1}
    val G: Array[Double] = Array.tabulate(es.length){i => es(i)._2}
    val start: Array[Double] = Array.fill(3)(0)
    start(0) = D.foldLeft(D(0)) { case (maxM, e) => math.max(maxM, e) }
    val Z: Array[Double] = Array.tabulate(pts.length){i => pts(i).data}
    start(1) = math.pow(stdev(Z), 2)
    start(2) = math.max(0, G.foldLeft(D(0)) { case (minM, e) => math.min(minM, e) })
    Semivariogram.fit(es, model, start)
  }
}
