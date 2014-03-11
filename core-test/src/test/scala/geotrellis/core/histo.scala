/***
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
 ***/

package geotrellis.core

import math.round

import scala.util.Random
import Console.printf

import org.scalatest.FunSpec
import org.scalatest.matchers.MustMatchers
import org.scalatest.matchers.ShouldMatchers
import geotrellis.statistics._

class HistogramSpec extends FunSpec with MustMatchers with ShouldMatchers {
  def stringToInts(s:String) = {
    s.toCharArray.map { _.toByte - 32 }
  }
  val kinds = List(("ArrayHistogram", ArrayHistogram, () => {ArrayHistogram(100)}),
                   ("MapHistogram", MapHistogram, () => {MapHistogram()}),
                   ("FastMapHistogram", FastMapHistogram, () => {FastMapHistogram()}))
  kinds.foreach {
    case (name, cls, builder) => {
      describe("A " + name) {
        it("should build") {
          builder()
        }
    
        it("should know its min and max values") {
          val h = builder()
    
          h.countItem(4)
          h.countItem(13)
          h.countItem(84)
    
          h.getMinValue must be === 4
          h.getMaxValue must be === 84
        }
    
        // REFACTOR: this should use option
        it("should behave predictably when empty") {
          val h = builder()
          // min value should be largest possible int
          // max value should be smallest possible int
          // this way it signals that the values don't really make sense
          h.getMinValue must be === Int.MaxValue
          h.getMaxValue must be === Int.MinValue
        }
    
        it("should store values and retrieve them later") {
          val h = builder()
    
          val s = "This is some great test data--see?"
          stringToInts(s).foreach { i => h.countItem(i) }
    
          h.getItemCount('z'.toByte - 32) must be === 0
          h.getItemCount('?'.toByte - 32) must be === 1
          h.getItemCount('T'.toByte - 32) must be === 1
          h.getItemCount('i'.toByte - 32) must be === 2
          h.getItemCount('s'.toByte - 32) must be === 5
          h.getItemCount(' '.toByte - 32) must be === 5
        }
    
        it("should do fancy kinds of counting and uncounting") {
          val h = builder()
          h.countItem(6, 30)
          h.countItem(8, 12)
          h.countItem(16, 20)
          h.uncountItem(16)
    
          h.getTotalCount must be === 42
          h.getMinValue must be === 6
          h.getMaxValue must be === 8
        }
        
        it("should generate quantile breaks") {
          val h = builder()
    
          val s = "weio gwej ah;iodfbo;dzfb;oaerge4;oi 34ch4oinj;;bjsdon;bsd; " +
          "jioijo43hi4oiernhow4y734herojniejnibodf[h0shemjhermjbdfmb j;jgj;gseg" +
          "43wjtnejinherhoe9980437843t43n8hy8h89huntjhgfjogfdtgj895n34y8nt34tpn"
          stringToInts(s).foreach { i => h.countItem(i) }
          
          h.getQuantileBreaks(6) must be === Array(23, 67, 71, 73, 78, 90)
  
          //println(h.generateStatistics)
        }
  
        it("should handle quantile breaks with extreme values") {
          val h = ArrayHistogram(10)
          h.countItem(1, 10)
          h.countItem(2, 1000)
          h.countItem(3, 120)
          h.countItem(4, 100)
          h.countItem(5, 80)
          h.countItem(6, 20)
    
          h.getQuantileBreaks(3) must be === Array(2, 3, 6)
        }
    
        it("should handle quantile breaks with multiple extreme values") {
          val h = ArrayHistogram(10)
          h.countItem(1, 20)
          h.countItem(2, 60)
          h.countItem(3, 10000)
          h.countItem(4, 10)
          h.countItem(5, 10000)
          h.countItem(6, 70)
          h.countItem(7, 50)
    
          h.getQuantileBreaks(5) must be === Array(2, 3, 5, 6, 7)
        }
    
        // we should decide if/how we want to handle this...
        //it("should handle quantile breaks with multiple extreme levels") {
        //  val h = ArrayHistogram(10)
        //  h.countItem(1, 20)
        //  h.countItem(2, 60)
        //  h.countItem(3, 10000)
        //  h.countItem(4, 10)
        //  h.countItem(5, 1000000000)
        //  h.countItem(6, 70)
        //  h.countItem(7, 50)
        //
        //  h.getQuantileBreaks(5) must be === Array(2, 3, 5, 6, 7)
        //}
    
        //it("should fail to generate unknown kinds of quantile breaks") {
        //  val h = builder()
        //  h.countItem(3, 2)
        //  h.countItem(5, 3)
        //  h.countItem(8, 4)
        //  evaluating { h.getQuantileBreaks(2, 'bad) } should produce [Exception];
        //}
        
        it("should serialize to json") {
          val h = ArrayHistogram(10)
          h.countItem(3, 2)
          h.countItem(5, 3)
          h.countItem(8, 5)
          h.toJSON must be === "[[3,2],[5,3],[8,5]]"
        }
      }
    }
  }

  describe("A severely unbalanced histogram") {
    it("should handle a tricky double unbalanced starting values") {
      val h = ArrayHistogram(10)
      h.countItem(0, 100)
      h.countItem(1, 1000)
      h.countItem(2, 15)
      h.countItem(3, 10)
      h.countItem(4, 5)
      h.countItem(5, 10)
      h.getQuantileBreaks(4) must be === Array(0, 1, 2, 5)
    }
  
    it("should handle a tricky double unbalanced later values") {
      val h = ArrayHistogram(10)
      h.countItem(0, 10)
      h.countItem(1, 100)
      h.countItem(2, 1000)
      h.countItem(3, 15)
      h.countItem(4, 10)
      h.countItem(5, 5)
      h.getQuantileBreaks(4) must be === Array(0, 1, 2, 5)
    }
  }

  describe("A Histogram") {
    it("should be able to handle any number of quantiles") {
      val h = ArrayHistogram(10)
      h.countItem(1, 10)
      h.countItem(2, 10)
      h.countItem(3, 10)
      h.countItem(4, 10)
      h.countItem(5, 10)

      h.getQuantileBreaks(1) must be === Array(5)
      h.getQuantileBreaks(2) must be === Array(2,5)
      h.getQuantileBreaks(3) must be === Array(2,3,5)
      h.getQuantileBreaks(4) must be === Array(1,2,4,5)
      h.getQuantileBreaks(5) must be === Array(1,2,3,4,5)
      h.getQuantileBreaks(6) must be === Array(1,2,3,4,5)
      h.getQuantileBreaks(7) must be === Array(1,2,3,4,5)
      h.getQuantileBreaks(8) must be === Array(1,2,3,4,5)
      h.getQuantileBreaks(9) must be === Array(1,2,3,4,5)
      h.getQuantileBreaks(10) must be === Array(1,2,3,4,5)
    }
  }

  describe("The statistics generator") {
    it("should generate stats where there are zero values") {
      val h = ArrayHistogram(20)
      h.countItem(0, 0)
      h.countItem(1, 0)
      h.countItem(2, 10)
      h.countItem(3, 15)
      h.countItem(4, 0)
      h.countItem(5, 18)
      h.countItem(6, 9)
      h.countItem(7, 2)
      h.countItem(8, 0)
      h.countItem(9, 0)
  
      val stats = h.generateStatistics
      //println(stats)
      "%.3f".format(stats.mean) must be === "4.130"
      stats.median must be === 5
      stats.mode must be === 5
      "%.3f".format(stats.stddev) must be === "1.528"
      stats.zmin must be === 2
      stats.zmax must be === 7
    }
  }
    
  
               
  describe("A CompressedArrayHistogram") {
    it("should support different constructors") {
      val size = 1024
      val vmin = 0
      val vmax = 1024 * 1024
  
      val h1 = CompressedArrayHistogram(size)
      val h2 = CompressedArrayHistogram(vmin, vmax)
      val h3 = CompressedArrayHistogram(vmin, vmax, size)
    }
  
    it("should pull values into its limits") {
      val vmin = 0
      val vmax = 20
      val h = CompressedArrayHistogram(vmin, vmax)
  
      h.getMinValue must be === Int.MaxValue
      h.getMaxValue must be === Int.MinValue
  
  
    }
  
    it("should compress properly") {
      val vmin = 0
      val vmax = 100
      val size = 10
      val h = CompressedArrayHistogram(vmin, vmax, size)
  
      h.getItemCount(13) must be === 0
      h.getItemCount(15) must be === 0
      h.getItemCount(19) must be === 0
      
      h.countItem(13)
      h.countItem(15)
      h.countItem(19)
  
      for(i <- vmin until vmax) {
        val c  = h.compress(i)
        val d  = h.decompress(c)
        val dc = h.compress(d)
        val dd = h.decompress(dc)
        //if (c != dc)
        //  printf("%d -> %d -> %d -> %d; %d != %d\n", i, c, d, dc, c, dc)
        c must be === dc
      }
  
      h.getItemCount(13) must be === 0
      h.getItemCount(15) must be === 0
      h.getItemCount(19) must be === 3
    }
  
    it("should handle large values") {
      val seed = 0xdeadbeef
      val rng  = new Random(seed)
      val size = 512 * 512
      val n    = 10 * 1000 * 1000
      //val n    = 100 * 1000 * 1000
      val data = (0 until size).map { i => rng.nextInt(n) }
    
      val vmin = 0
      val vmax = 4000
      //val h = CompressedArrayHistogram(vmin, vmax)
  
      //val h = MapHistogram()
  
      val h = FastMapHistogram()
    
      val t0 = System.currentTimeMillis()
      data.foreach { i => h.countItem(i) }
      val t1 = System.currentTimeMillis()
    
      h.getMinValue
      h.getMaxValue
    
      val t2 = System.currentTimeMillis()
      val nearest = h.getQuantileBreaks(6)
      //val lower =   h.getLowerQuantileBreaks(6)
      val t3 = System.currentTimeMillis()
    
      printf("%dms to build, %dms to find min/max, %dms to generate\n", t1 - t0, t2 - t1, t3 - t2)
    
      //println(nearest.toList)
      //nearest.toList must be === List(1657026, 3326562, 5000550, 6676121, 8350394, 9999963)
      //lower.toList   must be === List(1656979, 3326562, 5000550, 6676117, 8350394, 9999963)
    }
  }
}
