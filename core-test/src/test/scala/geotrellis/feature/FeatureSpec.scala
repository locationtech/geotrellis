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

package geotrellis.feature

import geotrellis._
import geotrellis.feature.op.geometry._
import geotrellis.process._
import geotrellis.feature._
import geotrellis.testkit._
import math.{max,min,round}
import org.scalatest.FunSpec
import org.scalatest.matchers.MustMatchers
import org.scalatest.matchers.ShouldMatchers
import geotrellis.feature.op.geometry.GetEnvelope
import geotrellis.feature.op.geometry.Intersect

class FeatureSpec extends FunSpec 
                     with MustMatchers 
                     with ShouldMatchers 
                     with TestServer {

  val p = Point(1.0,2.0,"hi")
  val b = Buffer(p, 5.0, 8, EndCapRound) // same as Buffer(p, 5.0)
  val p2 = Point (3.0,2.0,"goodbye")
  val b2 = Buffer(p2, 5.0)
  val p3 = Point (4.0, 5.0, "foo")

  describe("Buffer") {
    it("should return the correct geometry") {
      val p1 = get(b)
      p1.geom.normalize
      val expected = "POLYGON ((-4 2.000000000000004, -3.9039264020161513 2.975451610080646, -3.6193976625564312 3.9134171618254543, -3.1573480615127227 4.777851165098017, -2.5355339059327315 5.535533905932743, -1.7778511650980038 6.157348061512732, -0.9134171618254394 6.6193976625564375, 0.0245483899193697 6.903926402016154, 1.0000000000000124 7, 1.9754516100806545 6.9039264020161495, 2.9134171618254623 6.619397662556429, 3.777851165098024 6.157348061512717, 4.535533905932749 5.535533905932725, 5.157348061512736 4.777851165097997, 5.619397662556441 3.913417161825431, 5.903926402016156 2.975451610080622, 6 2, 5.903926402016152 1.0245483899193588, 5.619397662556434 0.086582838174551, 5.157348061512726 -0.7778511650980109, 4.535533905932738 -1.5355339059327373, 3.7778511650980113 -2.1573480615127263, 2.913417161825449 -2.619397662556434, 1.9754516100806416 -2.903926402016152, 1.0000000000000002 -3, 0.024548389919359 -2.903926402016152, -0.9134171618254485 -2.619397662556434, -1.77785116509801 -2.1573480615127263, -2.5355339059327373 -1.5355339059327378, -3.1573480615127263 -0.7778511650980109, -3.619397662556434 0.0865828381745528, -3.903926402016153 1.0245483899193615, -4 2.000000000000004))"

      assert ( p1.geom.toText === expected )
    }
    it ("should assign the feature data of the first argument to the result") {
      val r2 = get(b2)
      assert (r2.data === "goodbye") 
    }
  } 

  describe("GetCentroid") {
    val c = GetCentroid(b)
    val r = get(c)
    it("should return the correct geometry") {
      assert(r.geom.toText === "POINT (1.0000000000000002 2)")
    }
    it("should assign the feature data of the first argument to the result") {
      assert(r.data === "hi")
    } 
  }

  describe("GetEnvelope") {
    val e = GetEnvelope(b)
    val r = get(e)
    it("should return the correct geometry") {
      assert( r.geom.toText === "POLYGON ((-4 -3, -4 7, 6 7, 6 -3, -4 -3))") 
    }

    it("should assign the feature data of the first argument to the result") {
      assert(r.data === "hi")
    }
  }
  
  describe("Intersect") {
    val i = Intersect(p,b) 
    val r = get(i)
    it("should return the correct geometry") {
      assert(r.geom.toText === "POINT (1 2)")
    }

    it("should assign the feature data of the first argument to the result") {
      assert(r.data === "hi")
    }
  }


  describe("Disjoint") {
    val d = Disjoint(p,b)
    val r = get(d) 
    it("should return the correct boolean value") {
      assert(r === false)
    }
  }

  describe("Contains") {
    val c = Contains(b,p)
    val r = get(c)
    it("should return the correct boolean value") {
      assert(r === true)
    }
  }

  describe("Intersects") {
    val i = Intersects(b,b2)
    val r = get(i)
    it("should return the correct boolean value") {
      assert(r === true)
    }
  }

  describe("Overlaps") {
    val i = Overlaps(b,b2)
    val r = get(i)
    it("should return the correct boolean value") {
      assert(r === true)
    }
  }

  describe("Covers") {
    val i = Covers(b,b2)
    val r = get(i)
    it("should return the correct boolean value") {
      assert(r === false)
    }
  }


  describe("Erase") { 
    val i = Erase(b,p)
    val r = get(i)
    it("should return the correct geometry") {
      val expected = "POLYGON ((6 2, 5.903926402016152 1.0245483899193588, 5.619397662556434 0.086582838174551, 5.157348061512726 -0.7778511650980109, 4.535533905932738 -1.5355339059327373, 3.7778511650980113 -2.1573480615127263, 2.913417161825449 -2.619397662556434, 1.9754516100806416 -2.903926402016152, 1.0000000000000002 -3, 0.024548389919359 -2.903926402016152, -0.9134171618254485 -2.619397662556434, -1.77785116509801 -2.1573480615127263, -2.5355339059327373 -1.5355339059327378, -3.1573480615127263 -0.7778511650980109, -3.619397662556434 0.0865828381745528, -3.903926402016153 1.0245483899193615, -4 2.000000000000004, -3.9039264020161513 2.975451610080646, -3.6193976625564312 3.9134171618254543, -3.1573480615127227 4.777851165098017, -2.5355339059327315 5.535533905932743, -1.7778511650980038 6.157348061512732, -0.9134171618254394 6.6193976625564375, 0.0245483899193697 6.903926402016154, 1.0000000000000124 7, 1.9754516100806545 6.9039264020161495, 2.9134171618254623 6.619397662556429, 3.777851165098024 6.157348061512717, 4.535533905932749 5.535533905932725, 5.157348061512736 4.777851165097997, 5.619397662556441 3.913417161825431, 5.903926402016156 2.975451610080622, 6 2))"
      assert(r.geom.toText === expected)
    } 
    it("should assign the feature data of the first argument to the result") {
      assert(r.data === "hi")
    }
  }


  describe("GetSymDifference") {
    val symDifference = GetSymDifference(b,b2)
    val r = get(symDifference)
    it("should return the correct geometry") {
      val expected = "MULTIPOLYGON (((2.0000000000000004 -2.896479729346215, 2.913417161825449 -2.619397662556434, 3.7778511650980113 -2.1573480615127263, 4.535533905932738 -1.5355339059327373, 5.157348061512726 -0.7778511650980109, 5.619397662556434 0.086582838174551, 5.903926402016152 1.0245483899193588, 6 2, 5.903926402016156 2.975451610080622, 5.619397662556441 3.913417161825431, 5.157348061512736 4.777851165097997, 4.535533905932749 5.535533905932725, 3.777851165098024 6.157348061512717, 2.9134171618254623 6.619397662556429, 2.000000000000005 6.896479729346215, 2.02454838991937 6.903926402016154, 3.0000000000000124 7, 3.9754516100806545 6.9039264020161495, 4.913417161825462 6.619397662556429, 5.777851165098024 6.157348061512717, 6.535533905932749 5.535533905932725, 7.157348061512736 4.777851165097997, 7.619397662556441 3.913417161825431, 7.903926402016156 2.975451610080622, 8 2, 7.903926402016152 1.0245483899193588, 7.619397662556434 0.086582838174551, 7.157348061512726 -0.7778511650980109, 6.535533905932738 -1.5355339059327373, 5.777851165098012 -2.1573480615127263, 4.913417161825449 -2.619397662556434, 3.9754516100806416 -2.903926402016152, 3.0000000000000004 -3, 2.0245483899193593 -2.903926402016152, 2.0000000000000004 -2.896479729346215)), ((2.0000000000000004 -2.896479729346215, 1.9754516100806416 -2.903926402016152, 1.0000000000000002 -3, 0.024548389919359 -2.903926402016152, -0.9134171618254485 -2.619397662556434, -1.77785116509801 -2.1573480615127263, -2.5355339059327373 -1.5355339059327378, -3.1573480615127263 -0.7778511650980109, -3.619397662556434 0.0865828381745528, -3.903926402016153 1.0245483899193615, -4 2.000000000000004, -3.9039264020161513 2.975451610080646, -3.6193976625564312 3.9134171618254543, -3.1573480615127227 4.777851165098017, -2.5355339059327315 5.535533905932743, -1.7778511650980038 6.157348061512732, -0.9134171618254394 6.6193976625564375, 0.0245483899193697 6.903926402016154, 1.0000000000000124 7, 1.9754516100806545 6.9039264020161495, 2.000000000000005 6.896479729346215, 1.0865828381745606 6.6193976625564375, 0.2221488349019962 6.157348061512732, -0.5355339059327315 5.535533905932743, -1.1573480615127227 4.777851165098017, -1.6193976625564312 3.9134171618254543, -1.9039264020161513 2.975451610080646, -2 2.000000000000004, -1.903926402016153 1.0245483899193615, -1.619397662556434 0.0865828381745528, -1.1573480615127263 -0.7778511650980109, -0.5355339059327373 -1.5355339059327378, 0.22214883490199 -2.1573480615127263, 1.0865828381745515 -2.619397662556434, 2.0000000000000004 -2.896479729346215)))"
      assert(r.geom.toText === expected)
    }
    it("should assign the feature data of the first argument to the result") {
      assert(r.data === "hi")
    }
  }


  describe ("Union") {
    val union = Union(b,b2)
    val r = get(union)
    it("should return the correct geometry") {
      val expected = "POLYGON ((2.0000000000000004 -2.896479729346215, 1.9754516100806416 -2.903926402016152, 1.0000000000000002 -3, 0.024548389919359 -2.903926402016152, -0.9134171618254485 -2.619397662556434, -1.77785116509801 -2.1573480615127263, -2.5355339059327373 -1.5355339059327378, -3.1573480615127263 -0.7778511650980109, -3.619397662556434 0.0865828381745528, -3.903926402016153 1.0245483899193615, -4 2.000000000000004, -3.9039264020161513 2.975451610080646, -3.6193976625564312 3.9134171618254543, -3.1573480615127227 4.777851165098017, -2.5355339059327315 5.535533905932743, -1.7778511650980038 6.157348061512732, -0.9134171618254394 6.6193976625564375, 0.0245483899193697 6.903926402016154, 1.0000000000000124 7, 1.9754516100806545 6.9039264020161495, 2.000000000000005 6.896479729346215, 2.02454838991937 6.903926402016154, 3.0000000000000124 7, 3.9754516100806545 6.9039264020161495, 4.913417161825462 6.619397662556429, 5.777851165098024 6.157348061512717, 6.535533905932749 5.535533905932725, 7.157348061512736 4.777851165097997, 7.619397662556441 3.913417161825431, 7.903926402016156 2.975451610080622, 8 2, 7.903926402016152 1.0245483899193588, 7.619397662556434 0.086582838174551, 7.157348061512726 -0.7778511650980109, 6.535533905932738 -1.5355339059327373, 5.777851165098012 -2.1573480615127263, 4.913417161825449 -2.619397662556434, 3.9754516100806416 -2.903926402016152, 3.0000000000000004 -3, 2.0245483899193593 -2.903926402016152, 2.0000000000000004 -2.896479729346215))"
      assert(r.geom.toText === expected) 
    }
    it("should assign the feature data of the first argument to the result") {
      assert(r.data === "hi")
    }
  }

  describe("Simplify") {
    val simplify = Simplify(Union(b,b2), 2.0)
    val r = get(simplify)
    it("should return the correct geometry") {
      val expected = "POLYGON ((2.0000000000000004 -2.896479729346215, -2.5355339059327373 -1.5355339059327378, -3.9039264020161513 2.975451610080646, 3.9754516100806545 6.9039264020161495, 7.903926402016152 1.0245483899193588, 2.0000000000000004 -2.896479729346215))"
      assert (r.geom.toText === expected) 
    }
  }

  describe("ConvexHull") {
    it("should return the correct geometry") {
      val l = Union(Union(p,p2),p3)
      val cv = ConvexHull(l)
      val expected = "POLYGON ((1 2, 4 5, 3 2, 1 2))"
      val r = get(cv) 
      assert(r.geom.toText === expected)
    }
  } 

  describe("GetDistance") {
    it("should return the correct distance") {
      val dOp = GetDistance(p,p2)
      val d = get(dOp)
      val expected = 2.0
      assert(d === expected) 
    }
  }

  describe("GetArea") {
    it("should return the correct area") {
      val aOp = GetArea(b)
      val a = get(aOp)
      
      // The Buffered polygon has 8 segments per quater circle
      val angleOfTriangle = 90.0 / 8.0
      val areaOfTriangle = 0.5*5*5*math.sin(math.toRadians(angleOfTriangle))
      val expected = areaOfTriangle * 8 * 4
      
      a should be (expected plusOrMinus 0.000001) 
    }
  }

  describe("FlattenGeometry and Filter") {
    it("can split and filter a multipolygon") {
       val flattenOp = FlattenGeometry(GetSymDifference(b,b2)) 
       val polygonSeq = get(flattenOp)
       val expected = List()
       assert(polygonSeq.length === 2)
       val filterOp = logic.Filter(flattenOp,Contains(_:Geometry[_],Point(-3.5, 1.5))) 
       val filteredSeq = get(filterOp)
       assert(filteredSeq.length === 1)
    }
  }

  describe("creating empties") {
    it("should create empty features") {
      val gc =   GeometryCollection.empty()
      val gc2 =  GeometryCollection.empty(1)
      val ls =   LineString.empty()
      val ls2 =  LineString.empty(1)
      val mls =  MultiLineString.empty()
      val mls2 = MultiLineString.empty(1)
      val mp =   MultiPoint.empty()
      val mp2 =  MultiPoint.empty(1)
      val mpl =  MultiPolygon.empty()
      val mpl2 = MultiPolygon.empty(1)
      val p =    Point.empty()
      val p2 =   Point.empty(1)
      val pl =   Polygon.empty()
      val pl2 =  Polygon.empty(1)
    }
  }
}
