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

import org.scalatest._
import geotrellis.testkit._
import org.apache.commons.math3.stat.regression.SimpleRegression

class SemivariogramSpec extends FunSpec 
                           with Matchers 
                           with TestEngine 
                           with TileBuilders {
  describe("Linear Semivariogram") {
    it("Semivariogram (Bucketed)") {
      val points = Seq[PointFeature[Double]](
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


      val semivariogram = Semivariogram(points,None,2,Linear)
      //val semivariogram = Semivariogram(points,None,2)(Linear)
      semivariogram(0) should be (sv(0) +- 0.0001)
      semivariogram(10) should be (sv(10) +- 0.0001)
    }

    it("Semivariogram (Bucketed w/ Limit)") {
      val points = Seq[PointFeature[Double]](
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

      val semivariogram = Semivariogram(points,limit,2,Linear)
      semivariogram(0) should be (sv(0) +- 0.0001)
      semivariogram(10) should be (sv(10) +- 0.0001)
    }

    it("Semivariogram (Non-Bucketed)") {
      val points = Seq[PointFeature[Double]](
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

      val semivariogram = Semivariogram(points,None,0,Linear)
      semivariogram(0) should be (sv(0) +- 0.0001)
      semivariogram(10) should be (sv(10) +- 0.0001)
    }

    it("Semivariogram (Non-Bucketed w/ Limit)") {
      val points = Seq[PointFeature[Double]](
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

      val semivariogram = Semivariogram(points,limit,0,Linear)
      semivariogram(0) should be (sv(0) +- 0.0001)
      semivariogram(10) should be (sv(10) +- 0.0001)
    }
  }

  describe("Non-linear optimizations") {

      it("Gaussian Semivariogram") {
        println("Gaussian")
        val sv: Double => Double = Semivariogram.explicitGaussian(15, 90, 18)
        val empiricalSemivariogram: Array[(Double, Double)] = Array((1.0,18.31928994121732), (3.0,20.823160381032732), (9.0,39.767304522885766))
        val semivariogramGaussian = Semivariogram.fit(empiricalSemivariogram, Gaussian)
        semivariogramGaussian(0) should be (sv(0) +- 0.0001)
        semivariogramGaussian(5) should be (sv(5) +- 0.0001)
        semivariogramGaussian(10) should be (sv(10) +- 0.0001)
      }

      it("Exponential Semivariogram") {
        println("Exponential")
        val sv: Double => Double = Semivariogram.explicitExponential(100, 120, 25.5)
        val empiricalSemivariogram: Array[(Double, Double)] = Array((1.0,28.29289707966598), (2.0,31.003251576288495), (3.0,33.63350299186894), (4.0,36.18601873022862), (5.0,38.663096227832035), (6.0,41.0669650216348), (7.0,43.399788755817326), (8.0,45.663667129210694), (9.0,47.86063778516738))
        val semivariogramExponential = Semivariogram.fit(empiricalSemivariogram, Exponential)
        semivariogramExponential(0) should be (sv(0) +- 0.0001)
        semivariogramExponential(5) should be (sv(5) +- 0.0001)
        semivariogramExponential(10) should be (sv(10) +- 0.0001)
      }

      ignore("Circular Semivariogram") {
        println("Circular")
        val sv: Double => Double = Semivariogram.explicitCircular(12, 20, 8)
        val empiricalSemivariogram: Array[(Double, Double)] = Array((1.0,20.595619656061288), (2.0,21.11136866311396), (3.0,21.54928499674624), (4.0,21.909871250250184), (5.0,22.191954561590926), (6.0,22.392304845413264), (7.0,22.50483897316257), (8.0,22.518980562769638), (9.0,22.41597098529099))
        val semivariogramCircular = Semivariogram.fit(empiricalSemivariogram, Circular)
        semivariogramCircular(0) should be (sv(0) +- 0.0001)
        semivariogramCircular(5) should be (sv(5) +- 0.0001)
        semivariogramCircular(10) should be (sv(10) +- 0.0001)

        //Failing
        //Fix the formula; the one at (http://resources.arcgis.com/en/help/main/10.1/index.html#//009z00000076000000) is apparently incorrect
        //Not much mention of this semivariogram exists; hence manually developing the equation of the same
      }

      ignore("Spherical Semivariogram") {
        println("Spherical")
        //val sv: Double => Double = Semivariogram.explicitSpherical(17769.160, 1554.658, 618.044)
        val sv: Double => Double = Semivariogram.explicitSpherical(12, 20, 8)
        val empiricalSemivariogram: Array[(Double, Double)] = Array((1.0,9.496527777777779),(2.0,10.972222222222221),(3.0,12.40625),(4.0,13.777777777777779),(5.0,15.065972222222221),(6.0,16.25),(7.0,17.30902777777778),(8.0,18.22222222222222),(9.0,18.96875),(10.0,19.52777777777778),(11.0,19.87847222222222),(12.0,20.0),(13.0,20.0),(14.0,20.0),(15.0,20.0),(16.0,20.0),(17.0,20.0),(18.0,20.0),(19.0,20.0),(20.0,20.0),(21.0,20.0))
        val semivariogramSpherical = Semivariogram.fit(empiricalSemivariogram, Spherical)
        semivariogramSpherical(0) should be (sv(0) +- 0.0001)
        semivariogramSpherical(5) should be (sv(5) +- 0.0001)
        semivariogramSpherical(10) should be (sv(10) +- 0.0001)

        //Failing
        //Correct dataset generation and manual checking of jacobian equations
      }

      it("Wave Semivariogram") {
        println("Wave")
        val sv: Double => Double = Semivariogram.explicitWave(0.6, 10.6, 10.0)
        val empiricalSemivariogram: Array[(Double, Double)] = Array((2.0,10.589533930796113), (3.0,10.589541310870281), (4.0,10.589551637728729), (5.0,10.589564906129878), (8.0,10.58962228581659), (9.0,10.589647238195898), (10.0,10.589675083622401), (11.0,10.589705807980492), (12.0,10.58973939570023), (14.0,10.589815091730422), (15.0,10.589857161718223), (16.0,10.589902018445489), (17.0,10.589949639228873), (18.0,10.59), (19.0,10.590053075319977), (20.0,10.590108838394725), (21.0,10.590167261091125), (22.0,10.590228313953954), (23.0,10.590291966223626), (24.0,10.5903581858547), (25.0,10.59042693953516), (26.0,10.590498192706432), (126.0,10.60142857142857))
        val semivariogramWave = Semivariogram.fit(empiricalSemivariogram, Wave)

        //1 % tolerance of the h-value, viz. the distance; unlike the other semivariograms, which are capturing the quality irrespective of h-value
        semivariogramWave(0) should be (sv(0) +- 0.0001)
        semivariogramWave(5) should be (sv(5) +- 0.0001)
        semivariogramWave(10) should be (sv(10) +- 0.001)
        semivariogramWave(100) should be (sv(100) +- 0.01)
      }
    }
}
