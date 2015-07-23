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

import geotrellis.testkit._
import geotrellis.vector.{Point, PointFeature}
import org.scalatest._

class SemivariogramSpec extends FunSpec 
                           with Matchers 
                           with TestEngine 
                           with TileBuilders {
  describe("Linear Semivariogram") {
    it("Semivariogram (Bucketed)") {
      val points = Array[PointFeature[Double]](
        PointFeature(Point(0.0,0.0),10),
        PointFeature(Point(1.0,0.0),20),
        PointFeature(Point(4.0,4.0),60),
        PointFeature(Point(0.0,6.0),80)
        )

      /* would make pairs:
        1. (0,0,10) (1,0,20) // dist: 1.0
        2. (0,0,10) (4,4,60) // dist: 5.657
        3. (0,0,10) (0,6,80) // dist: 6
        4. (1,0,20) (4,4,60) // dist: 5
        5. (1,0,20) (0,6,80) // dist: 6.083
        6. (4,4,60) (0,6,80) // dist: 4.472

        Buckets:
          {0,2} // sv: 50   <-(20-10)^2 / 2
          {2,4} // sv: NaN
          {4,6} // sv: 750  <-((60-10)^2 + (60-20)^2 + (80-60)^2) / 3 / 2
          {6,8} // sv: 2125 <-((80-10)^2 + (80-20)^2) / 2 / 2

        Regression on points:
          {1,50}, {5,750}, {7,2125}
      */

      val slope = 321.42857
      val intercept = -417.85714
      val sv = (x:Double) => slope*x + intercept

      val semivariogram = LinearSemivariogram(points, None, 2)
      semivariogram(0) should be (sv(0) +- 0.0001)
      semivariogram(10) should be (sv(10) +- 0.0001)
    }

    it("Semivariogram (Bucketed w/ Limit)") {
      val points = Array[PointFeature[Double]](
        PointFeature(Point(0.0,0.0),10),
        PointFeature(Point(1.0,0.0),20),
        PointFeature(Point(4.0,4.0),60),
        PointFeature(Point(0.0,6.0),80)
        )

      /* would make pairs:
        1. (0,0,10) (1,0,20) // dist: 1.0
        2. (0,0,10) (4,4,60) // dist: 5.657
        3. (0,0,10) (0,6,80) // dist: 6
        4. (1,0,20) (4,4,60) // dist: 5
        5. (4,4,60) (0,6,80) // dist: 4.472

        removed:
        X. (1,0,20) (0,6,80) // dist: 6.083

        Buckets:
          {0,2} // sv: 50   <-(20-10)^2 / 2
          {2,4} // sv: NaN
          {4,6} // sv: 750  <-((60-10)^2 + (60-20)^2 + (80-60)^2) / 3 / 2
          {6,8} // sv: 2450 <-(80-10)^2 / 2

        Regression on points:
          {1,50}, {5,750}, {7,2450}
      */

      val slope = 367.85714
      val intercept = -510.71429
      val sv = (x:Double) => slope*x + intercept
      val limit:Option[Double] = Some(6)

      val semivariogram = LinearSemivariogram(points,limit,2)
      semivariogram(0) should be (sv(0) +- 0.0001)
      semivariogram(10) should be (sv(10) +- 0.0001)
    }

    it("Semivariogram (Non-Bucketed)") {
      val points = Array[PointFeature[Double]](
        PointFeature(Point(0.0,0.0),10),
        PointFeature(Point(0.0,0.0),16),
        PointFeature(Point(1.0,0.0),20),
        PointFeature(Point(0.0,1.0),24),
        PointFeature(Point(2.0,2.0),50)
        )

      /* would make pairs:
        1. (0,0,10) (0,0,16) // dist: 0
        2. (0,0,10) (1,0,20) // dist: 1
        3. (0,0,10) (0,1,24) // dist: 1
        4. (0,0,10) (2,2,50) // dist: srt(8)
        5. (0,0,16) (1,0,20) // dist: 1
        6. (0,0,16) (0,1,24) // dist: 1
        7. (0,0,16) (2,2,50) // dist: srt(8)
        8. (1,0,20) (0,1,24) // dist: sqrt(2)
        9. (1,0,20) (2,2,50) // dist: srt(5)
        10.(0,1,24) (2,2,50) // dist: srt(5)

        dist: 0 // sv: 18 <-(16-10)^2 / 2
        dist: 1 // sv: 47 <-((20-10)^2 + (24-10)^2 + (20-16)^2 + (24-16)^2) / 4 / 2
        dist: sqrt(2) // sv: 8 <-((24-20)^2 / 2
        dist: sqrt(5) // sv: 394 <-((50-20)^2 + (50-24)^2) / 2 / 2
        dist: sqrt(8) // sv: 689 <-((50-10)^2 + (50-16)^2) / 2 / 2

        Regression on points:
          {0,18}
          {1,47}
          {sqrt(2),8}
          {sqrt(5),394}
          {sqrt(8),689}
      */

      val slope = 240.77389
      val intercept = -128.93555
      val sv = (x:Double) => slope*x + intercept

      val semivariogram = LinearSemivariogram(points,None,0)
      semivariogram(0) should be (sv(0) +- 0.0001)
      semivariogram(10) should be (sv(10) +- 0.0001)
    }

    it("Semivariogram (Non-Bucketed w/ Limit)") {
      val points = Array[PointFeature[Double]](
        PointFeature(Point(0.0,0.0),10),
        PointFeature(Point(0.0,0.0),16),
        PointFeature(Point(1.0,0.0),20),
        PointFeature(Point(0.0,1.0),24),
        PointFeature(Point(2.0,2.0),50)
        )

      /* would make pairs:
        1. (0,0,10) (0,0,16) // dist: 0
        2. (0,0,10) (1,0,20) // dist: 1
        3. (0,0,10) (0,1,24) // dist: 1

        4. (0,0,16) (1,0,20) // dist: 1
        5. (0,0,16) (0,1,24) // dist: 1

        6. (1,0,20) (0,1,24) // dist: sqrt(2)
        7. (1,0,20) (2,2,50) // dist: srt(5)
        8. (0,1,24) (2,2,50) // dist: srt(5)

        removed:
        X. (0,0,10) (2,2,50) // dist: srt(8)
        X. (0,0,16) (2,2,50) // dist: srt(8)

        dist: 0 // sv: 18 <-(16-10)^2 / 2
        dist: 1 // sv: 47 <-((20-10)^2 + (24-10)^2 + (20-16)^2 + (24-16)^2) / 4 / 2
        dist: sqrt(2) // sv: 8 <-((24-20)^2 / 2

        Regression on points:
          {0,18}
          {1,47}
          {sqrt(2),8}
      */

      val slope = -0.40878
      val intercept = 24.66229
      val sv = (x:Double) => slope*x + intercept
      val limit:Option[Double] = Some(2)

      val semivariogram = LinearSemivariogram(points,limit,0)
      semivariogram(0) should be (sv(0) +- 0.0001)
      semivariogram(10) should be (sv(10) +- 0.0001)
    }
  }

  describe("Non-linear optimizations") {
    it("Gaussian Semivariogram w/o starting point") {
      val sv: Semivariogram = NonLinearSemivariogram(15, 90, 18, Gaussian)
      val empiricalSemivariogram: Array[(Double, Double)] = Array((1.0,18.31928994121732),
                                                                  (3.0,20.823160381032732),
                                                                  (9.0,39.767304522885766))
      val semivariogramGaussian = Semivariogram.fit(empiricalSemivariogram, Gaussian)
      semivariogramGaussian(0) should be (sv(0) +- 0.0001)
      semivariogramGaussian(5) should be (sv(5) +- 0.0001)
      semivariogramGaussian(10) should be (sv(10) +- 0.0001)
    }

    it("Gaussian Semivariogram w/ starting point") {
      val start: Array[Double] = Array[Double](1, 1, 1)
      val sv: Semivariogram = NonLinearSemivariogram(15, 90, 18, Gaussian)
      val empiricalSemivariogram: Array[(Double, Double)] = Array((1.0,18.31928994121732),
                                                                  (3.0,20.823160381032732),
                                                                  (9.0,39.767304522885766))
      val semivariogramGaussian = Semivariogram.fit(empiricalSemivariogram, Gaussian, start)
      semivariogramGaussian(0) should be (sv(0) +- 0.0001)
      semivariogramGaussian(5) should be (sv(5) +- 0.0001)
      semivariogramGaussian(10) should be (sv(10) +- 0.0001)
    }

    it("Exponential Semivariogram w/o starting point") {
      val sv: Semivariogram = NonLinearSemivariogram(100, 120, 25.5, Exponential)
      val empiricalSemivariogram: Array[(Double, Double)] = Array((1.0,28.29289707966598),
                                                                  (2.0,31.003251576288495),
                                                                  (8.0,45.663667129210694),
                                                                  (9.0,47.86063778516738))
      val semivariogramExponential = Semivariogram.fit(empiricalSemivariogram, Exponential)
      semivariogramExponential(0) should be (sv(0) +- 0.0001)
      semivariogramExponential(5) should be (sv(5) +- 0.0001)
      semivariogramExponential(10) should be (sv(10) +- 0.0001)
    }

    it("Exponential Semivariogram w/ starting point") {
      val start: Array[Double] = Array[Double](1, 1, 1)
      val sv: Semivariogram = NonLinearSemivariogram(100, 120, 25.5, Exponential)
      val empiricalSemivariogram: Array[(Double, Double)] = Array((1.0,28.29289707966598),
                                                                  (2.0,31.003251576288495),
                                                                  (8.0,45.663667129210694),
                                                                  (9.0,47.86063778516738))
      val semivariogramExponential = Semivariogram.fit(empiricalSemivariogram, Exponential, start)
      semivariogramExponential(0) should be (sv(0) +- 0.0001)
      semivariogramExponential(5) should be (sv(5) +- 0.0001)
      semivariogramExponential(10) should be (sv(10) +- 0.0001)
    }

    ignore("Circular Semivariogram w/o starting point") {

      //This fails, since NaN starts appearing in the Jacobian matrix while searching for the solution.
      //So, in such cases, the user should supply an adequate starting point looking at the sample data; which will help
      //in obtaining the appropriate optimization

      //Next test case handles the same problem with an appropriate guess

      val sv: Semivariogram = NonLinearSemivariogram(12, 20, 8, Circular)
      val empiricalSemivariogram: Array[(Double, Double)] = Array((1.0,9.271764348975337),
                                                                  (2.0,10.534640218523169),
                                                                  (9.0,18.26847664622735),
                                                                  (10.0,19.044740230185127),
                                                                  (11.0,19.657832500414067))
      val start: Array[Double] = Array[Double](12, 22, 10)
      val semivariogramCircular = Semivariogram.fit(empiricalSemivariogram, Circular)
      semivariogramCircular(0) should be (sv(0) +- 0.0001)
      semivariogramCircular(5) should be (sv(5) +- 0.0001)
      semivariogramCircular(10) should be (sv(10) +- 0.0001)
    }

    it("Circular Semivariogram w/ starting point") {
      val start: Array[Double] = Array[Double](12, 22, 10)
      val sv: Semivariogram = NonLinearSemivariogram(12, 20, 8, Circular)
      val empiricalSemivariogram: Array[(Double, Double)] = Array((1.0,9.271764348975337),
                                                                  (2.0,10.534640218523169),
                                                                  (9.0,18.26847664622735),
                                                                  (10.0,19.044740230185127),
                                                                  (11.0,19.657832500414067))
      val semivariogramCircular = Semivariogram.fit(empiricalSemivariogram, Circular, start)
      semivariogramCircular(0) should be (sv(0) +- 0.0001)
      semivariogramCircular(5) should be (sv(5) +- 0.0001)
      semivariogramCircular(10) should be (sv(10) +- 0.0001)
    }

    it("Spherical Semivariogram w/o starting point") {
      val sv: Semivariogram = NonLinearSemivariogram(6, 4, 1, Spherical)
      val empiricalSemivariogram: Array[(Double, Double)] = Array((0.7,1.522618),
                                                                  (0.8,1.596444),
                                                                  (2.0,2.444444),
                                                                  (2.1,2.510687),
                                                                  (3.4,3.277055),
                                                                  (3.5,3.327256),
                                                                  (3.9,3.513062),
                                                                  (4.0,3.555555))
      val semivariogramSpherical = Semivariogram.fit(empiricalSemivariogram, Spherical)

      semivariogramSpherical(0) should be (sv(0) +- 0.0001)
      semivariogramSpherical(3.2) should be (sv(3.2) +- 0.0001)
      semivariogramSpherical(5) should be (sv(5) +- 0.0001)
      semivariogramSpherical(10) should be (sv(10) +- 0.0001)
    }

    it("Spherical Semivariogram w/ starting point") {
      val start: Array[Double] = Array[Double](1, 1, 1)
      val sv: Semivariogram = NonLinearSemivariogram(6, 4, 1, Spherical)
      val empiricalSemivariogram: Array[(Double, Double)] = Array((0.7,1.522618),
                                                                  (0.8,1.596444),
                                                                  (2.0,2.444444),
                                                                  (2.1,2.510687),
                                                                  (3.4,3.277055),
                                                                  (3.5,3.327256),
                                                                  (3.9,3.513062),
                                                                  (4.0,3.555555))
      val semivariogramSpherical = Semivariogram.fit(empiricalSemivariogram, Spherical, start)

      semivariogramSpherical(0) should be (sv(0) +- 0.0001)
      semivariogramSpherical(3.2) should be (sv(3.2) +- 0.0001)
      semivariogramSpherical(5) should be (sv(5) +- 0.0001)
      semivariogramSpherical(10) should be (sv(10) +- 0.0001)
    }

    it("Wave Semivariogram w/o starting point") {
      val (r, s, a) = (0.6, 0.6, 0)
      val sv: Semivariogram = NonLinearSemivariogram(r, s, a, Wave)
      val empiricalSemivariogram: Array[(Double, Double)] = Array((0.09,0.002247470105603111),
                                                                  (0.16,0.007085869927046118),
                                                                  (0.2,0.011049545766925961),
                                                                  (0.27,0.02004595451835964),
                                                                  (0.32,0.02804263020228621),
                                                                  (0.35,0.03345350137340246),
                                                                  (0.36,0.03535752660496465),
                                                                  (0.4,0.04346717723723661),
                                                                  (0.42,0.04781341093912201),
                                                                  (0.48,0.06198293182535792),
                                                                  (0.49,0.06450536228841193),
                                                                  (0.5,0.06707266569885333))
      val semivariogramWave = Semivariogram.fit(empiricalSemivariogram, Wave)

      semivariogramWave(0) should be (sv(0) +- 0.0001)
      semivariogramWave(5) should be (sv(5) +- 0.0001)
      semivariogramWave(10) should be (sv(10) +- 0.0001)
      semivariogramWave(100) should be (sv(100) +- 0.0001)
    }

    it("Wave Semivariogram w/ starting point") {
      val start: Array[Double] = Array[Double](1, 1, 1)
      val sv: Semivariogram = NonLinearSemivariogram(0.6, 0.6, 0, Wave)
      val empiricalSemivariogram: Array[(Double, Double)] = Array((0.09,0.002247470105603111),
                                                                  (0.16,0.007085869927046118),
                                                                  (0.2,0.011049545766925961),
                                                                  (0.27,0.02004595451835964),
                                                                  (0.32,0.02804263020228621),
                                                                  (0.35,0.03345350137340246),
                                                                  (0.36,0.03535752660496465),
                                                                  (0.4,0.04346717723723661),
                                                                  (0.42,0.04781341093912201),
                                                                  (0.48,0.06198293182535792),
                                                                  (0.49,0.06450536228841193),
                                                                  (0.5,0.06707266569885333))
      val semivariogramWave = Semivariogram.fit(empiricalSemivariogram, Wave, start)

      semivariogramWave(0) should be (sv(0) +- 0.0001)
      semivariogramWave(5) should be (sv(5) +- 0.0001)
      semivariogramWave(10) should be (sv(10) +- 0.0001)
      semivariogramWave(100) should be (sv(100) +- 0.0001)
    }
  }
}
