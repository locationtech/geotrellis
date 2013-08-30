package geotrellis.testutil

import geotrellis.network._
import geotrellis.network.graph._

import com.github.mdr.ascii._

import scala.collection.mutable

case class ShortestPathTestCase(graph:TransitGraph, source:Int, expected:Map[Location,Int])

object ShortestPathGraphs {
  def simple = {
    val graph = TestGraph.fromDiagram("""
     +-+   [7]    +-+
 ----|1|--------->|2|----
 |   +-+          +-+   |
 |    | [9]   [10] |    |[15]
 |    ------ -------    |
 |[14]     | |          |
 v         v v          v
+-+  [2] +----+  [11]  +-+
|6|<-----| 3  |------->|4|
+-+      +----+        +-+
 |                      |
 |[9]    +-+    [6]     |
 ------->|5|<------------
         +-+
                                      """, s => Location(s.toDouble,s.toDouble))
    val shortestPaths = Map[Location,Int](
      Location(1.0,1.0) -> 0,
      Location(2.0,2.0) -> 7,
      Location(3.0,3.0) -> 9,
      Location(4.0,4.0) -> 20,
      Location(5.0,5.0) -> 20,
      Location(6.0,6.0) -> 11
    )
    val packed = graph.pack
    val source = packed.vertexAt(Location(1.0,1.0))
    ShortestPathTestCase(packed,source,shortestPaths)
  }

  def arrival = {
    /*
     Two paths:
     B ->10minWalk-> C ->10minWalk-> D ->50minWalk->G
     B ->5minWalk-> A ->10minTrain(11:00)-> E ->20minTraint(11:20)-> F ->1minWalk -> G

     Arrive by 12:00, should choose transit
     */

    val a = StationVertex(Location(1.0,1.0),"A")
    val b = StreetVertex(Location(2.0,2.0),"B")
    val c = StreetVertex(Location(3.0,3.0),"C")
    val d = StreetVertex(Location(4.0,4.0),"D")
    val e = StationVertex(Location(5.0,5.0),"E")
    val f = StationVertex(Location(6.0,6.0),"F")
    val g = StreetVertex(Location(7.0,7.0),"G")

    val graph = MutableGraph(Seq(a,b,c,d,e,f,g))

    graph.addEdge(b,WalkEdge(c,Duration.fromMinutes(10)))
    graph.addEdge(c,WalkEdge(b,Duration.fromMinutes(10)))
    graph.addEdge(c,WalkEdge(d,Duration.fromMinutes(10)))
    graph.addEdge(d,WalkEdge(c,Duration.fromMinutes(10)))
    graph.addEdge(d,WalkEdge(g,Duration.fromMinutes(50)))
    graph.addEdge(g,WalkEdge(d,Duration.fromMinutes(50)))

    graph.addEdge(b,WalkEdge(a,Duration.fromMinutes(5)))
    graph.addEdge(a,WalkEdge(b,Duration.fromMinutes(5)))
    graph.addEdge(a,TransitEdge(e,"test",Time.parse("11:00:00"),Duration.fromMinutes(10)))
    graph.addEdge(e,TransitEdge(f,"test",Time.parse("11:20:00"),Duration.fromMinutes(20)))
    graph.addEdge(f,WalkEdge(g,Duration.fromMinutes(1)))
    graph.addEdge(g,WalkEdge(f,Duration.fromMinutes(1)))

    (graph,g,b)
  }
}

object SampleGraph {
  def noTimes = {
    TestGraph.fromDiagram("""
     +-+   [7]    +-+
 ----|1|--------->|2|----           +-+  [10]   +-+
 |   +-+          +-+   |           |8|---------|9|
 |    | [10]   [9] |    |[14]       +-+         +-+
 |    ------ -------    |            |           |
 |[15]     | |          |            |[8]        |
 v         v v          v            |           |
+-+      +----+  [3]   +-+    [10]  +-+          |
|6|      | 3  |------->|4|----------|7|          |[22]
+-+      +----+        +-+          +-+          |
 |                      |                        |
 |[6]    +-+    [9]     |                        |
 ------->|5|<------------                        |
         +-+                                     |
          ^                                      |
          |                                      |
          ----------------------------------------
                           """, s => Location(s.toDouble,s.toDouble))
  }

  def withTimes = {
    val vertices = List(
      StreetVertex(Location(1.0,1.0),"1"),
      StreetVertex(Location(2.0,1.0),"2"),
      StreetVertex(Location(3.0,1.0),"3"),
      StreetVertex(Location(4.0,1.0),"4"),
      StreetVertex(Location(5.0,1.0),"5"),
      StreetVertex(Location(6.0,1.0),"6"),
      StreetVertex(Location(7.0,1.0),"7"),
      StreetVertex(Location(8.0,1.0),"8"),
      StreetVertex(Location(9.0,1.0),"9"),
      StreetVertex(Location(10.0,1.0),"10")
    )

    val g = MutableGraph(vertices.toSeq)

    for(u <- vertices) {
      for(v <- vertices) {
        if(u != v) {
          g.addEdge(u,TransitEdge(v,"test",Time(u.location.lat.toInt*10),Duration(u.location.lat.toInt)))
        }
      }
    }

    g
  }

  def withTimesAndAnyTimes = {
    val vertices = List(
      StreetVertex(Location(1.0,1.0),"1"),
      StreetVertex(Location(2.0,1.0),"2"),
      StreetVertex(Location(3.0,1.0),"3"),
      StreetVertex(Location(4.0,1.0),"4"),
      StreetVertex(Location(5.0,1.0),"5"),
      StreetVertex(Location(6.0,1.0),"6"),
      StreetVertex(Location(7.0,1.0),"7"),
      StreetVertex(Location(8.0,1.0),"8"),
      StreetVertex(Location(9.0,1.0),"9"),
      StreetVertex(Location(10.0,1.0),"10")
    )

    val g = MutableGraph(vertices.toSeq)

    for(u <- vertices) {
      for(v <- vertices) {
        if(u != v) {
          g.addEdge(u,TransitEdge(v,"test",Time(u.location.lat.toInt*10),Duration(u.location.lat.toInt)))
          g.addEdge(u,WalkEdge(v,Duration(20)))
        }
      }
    }

    g
  }
}

object TestGraph {
  def fromDiagram(diagram:String):MutableGraph = fromDiagram(diagram, s => Location(0.0,0.0))

  def fromDiagram(diagram:String, locationSet:(String)=>Location):MutableGraph = {
    val g = MutableGraph()
    val d = Diagram(diagram)
    val vertices = new mutable.HashMap[Box,Vertex]()
    d.allBoxes.map { b =>
      if(!vertices.contains(b)) { 
        vertices(b) = StreetVertex(locationSet(b.text),b.text) 
        g += vertices(b)
      }
      
      b.edges
       .filter { e =>
         if(e.box1 == b) {
           e.hasArrow2
         } else {
           e.hasArrow1
         }
       }
       .foreach { e =>
         val targetBox = 
           if(e.box1 == b) {
             e.box2
           } else {
             e.box1
           }

         if(!vertices.contains(targetBox)) { 
           vertices(targetBox) = StreetVertex(locationSet(targetBox.text),targetBox.text) 
           g += vertices(targetBox)
         }

         val weight = Duration(e.label.get.toInt)
         g.addEdge(vertices(b),WalkEdge(vertices(targetBox),weight))
       }
     }

    g
  }
}
