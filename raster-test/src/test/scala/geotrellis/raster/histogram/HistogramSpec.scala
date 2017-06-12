/*
 * Copyright 2016 Azavea
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package geotrellis.raster.histogram

import org.scalatest._

import java.util.Locale


class HistogramSpec extends FunSpec with Matchers with Inspectors {
  private def charToInt(c: Char) = c.toByte - 32
  private def stringToInts(s:String) = {
    s.toCharArray.map(charToInt)
  }
  val kinds = List(("FastMapHistogram", FastMapHistogram, () => {FastMapHistogram()}))
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

          h.minValue.get should be (4)
          h.maxValue.get should be (84)
        }

        it("should behave predictably when empty") {
          val h = builder()
          // min value should be largest possible int
          // max value should be smallest possible int
          // this way it signals that the values don't really make sense
          h.minValue should be (None)
          h.maxValue should be (None)
        }

        it("should store values and retrieve them later") {
          val h = builder()

          val s = "This is some great test data--see?"
          stringToInts(s).foreach { i => h.countItem(i) }

          val expected = Seq(
            ('z', 0),
            ('?', 1),
            ('T', 1),
            ('i', 2),
            ('s', 5),
            (' ', 5)
          )

          forAll(expected) { case (char, count) ⇒
            h.itemCount(charToInt(char)) should be (count)
          }

          val bins = h.binCounts()

          bins.length should be (s.distinct.length)

          bins.map(_._2).sum should be (s.length)

          forAll(expected.filter(_._2 > 0)) { case (char, count) ⇒
            val label = charToInt(char)
            bins.find(_._1 == label) should be ('nonEmpty)
          }
        }

        it("should do fancy kinds of counting and uncounting") {
          val h = builder()
          h.countItem(6, 30)
          h.countItem(8, 12)
          h.countItem(16, 20)
          h.uncountItem(16)

          h.totalCount should be (42)
          h.minValue.get should be (6)
          h.maxValue.get should be (8)
        }

        it("should generate quantile breaks") {
          val h = builder()

          val s = "weio gwej ah;iodfbo;dzfb;oaerge4;oi 34ch4oinj;;bjsdon;bsd; " +
          "jioijo43hi4oiernhow4y734herojniejnibodf[h0shemjhermjbdfmb j;jgj;gseg" +
          "43wjtnejinherhoe9980437843t43n8hy8h89huntjhgfjogfdtgj895n34y8nt34tpn"
          stringToInts(s).foreach { i => h.countItem(i) }

          h.quantileBreaks(6) should be (Array(23, 67, 71, 73, 78, 90))
        }

        it("should handle quantile breaks with extreme values") {
          val h = FastMapHistogram()
          h.countItem(1, 10)
          h.countItem(2, 1000)
          h.countItem(3, 120)
          h.countItem(4, 100)
          h.countItem(5, 80)
          h.countItem(6, 20)

          h.quantileBreaks(3) should be (Array(2, 3, 6))
        }

        it("should handle quantile breaks with multiple extreme values") {
          val h = FastMapHistogram()
          h.countItem(1, 20)
          h.countItem(2, 60)
          h.countItem(3, 10000)
          h.countItem(4, 10)
          h.countItem(5, 10000)
          h.countItem(6, 70)
          h.countItem(7, 50)

          h.quantileBreaks(5) should be (Array(2, 3, 5, 6, 7))
        }
      }
    }
  }

  describe("A severely unbalanced histogram") {
    it("should handle a tricky double unbalanced starting values") {
      val h = FastMapHistogram()
      h.countItem(0, 100)
      h.countItem(1, 1000)
      h.countItem(2, 15)
      h.countItem(3, 10)
      h.countItem(4, 5)
      h.countItem(5, 10)
      h.quantileBreaks(4) should be (Array(0, 1, 2, 5))
    }

    it("should handle a tricky double unbalanced later values") {
      val h = FastMapHistogram()
      h.countItem(0, 10)
      h.countItem(1, 100)
      h.countItem(2, 1000)
      h.countItem(3, 15)
      h.countItem(4, 10)
      h.countItem(5, 5)
      h.quantileBreaks(4) should be (Array(0, 1, 2, 5))
    }
  }

  describe("A Histogram") {
    it("should be able to handle any number of quantiles") {
      val h = FastMapHistogram()
      h.countItem(1, 10)
      h.countItem(2, 10)
      h.countItem(3, 10)
      h.countItem(4, 10)
      h.countItem(5, 10)

      h.quantileBreaks(1) should be (Array(5))
      h.quantileBreaks(2) should be (Array(2,5))
      h.quantileBreaks(3) should be (Array(2,3,5))
      h.quantileBreaks(4) should be (Array(1,2,4,5))
      h.quantileBreaks(5) should be (Array(1,2,3,4,5))
      h.quantileBreaks(6) should be (Array(1,2,3,4,5))
      h.quantileBreaks(7) should be (Array(1,2,3,4,5))
      h.quantileBreaks(8) should be (Array(1,2,3,4,5))
      h.quantileBreaks(9) should be (Array(1,2,3,4,5))
      h.quantileBreaks(10) should be (Array(1,2,3,4,5))
    }
  }

  describe("The statistics generator") {
    it("should generate stats where there are zero values") {
      val h = FastMapHistogram()
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

      val stats = h.statistics.get
      stats should not be (None)

      //println(stats)
      "%.3f".formatLocal(Locale.ENGLISH, stats.mean) should be ("4.130")
      stats.median should be (5)
      stats.mode should be (5)
      "%.3f".formatLocal(Locale.ENGLISH, stats.stddev) should be ("1.528")
      stats.zmin should be (2)
      stats.zmax should be (7)
    }
  }

}
