package geotrellis.network.graph

import geotrellis.network._
import geotrellis.testutil._

import org.scalatest.FunSpec
import org.scalatest.matchers.ShouldMatchers

import scala.collection.mutable

class PackedGraphSpec extends FunSpec
                         with ShouldMatchers {
  describe("PackedGraph") {
    it("should pack a graph correctly.") {
      val unpacked = SampleGraph.noTimes
      val packed = unpacked.pack()
      
      val packedToUnpacked = (for(v <- 0 until packed.vertexCount) yield {
        val location = packed.location(v)
        unpacked.vertices.find(_.location == location) match {
          case Some(vertex) =>
            (v,vertex)
          case _ =>
            sys.error(s"Could not find vertex $v in unpacked graph.")
        }
      }).toMap

      for(v <- packedToUnpacked.keys) {
        val unpackedEdges = unpacked.edges(packedToUnpacked(v))
        val packedEdges = mutable.ListBuffer[Edge]()
        packed.getEdgeIterator(PublicTransit(EveryDaySchedule),EdgeDirection.Outgoing)
              .foreachEdge(v,0) { (t,w) =>
                 packedEdges += WalkEdge(packedToUnpacked(t),Duration(w))
               }
        packedEdges.sortBy(_.target.location.lat).toSeq should be 
           (unpackedEdges.toSeq.sortBy(_.target.location.lat).toSeq)
      }
    }

    it("should return proper outgoing edges for times.") {
      val packed = SampleGraph.withTimes.pack()

      // No edges past time 100
      for(v <- 0 until packed.vertexCount) { 
        var c = 0
        packed.getEdgeIterator(PublicTransit(EveryDaySchedule),EdgeDirection.Outgoing)
              .foreachEdge(v, 101) { (t,w) => c += 1 }
                 c should be (0)
               }

      val v5 = packed.vertexAt(Location(5.0,1.0))
      packed.getEdgeIterator(PublicTransit(EveryDaySchedule),EdgeDirection.Outgoing)
            .foreachEdge(v5,20) { (t,w) =>
               w should be ((50-20) + 5)
             }

      val v7 = packed.vertexAt(Location(7.0,1.0))
      packed.getEdgeIterator(PublicTransit(EveryDaySchedule),EdgeDirection.Outgoing)
            .foreachEdge(v7,20) { (t,w) =>
               w should be ((70-20) + 7)
             }
    }

    // it("should return proper outgoing edges for times and any times.") {
    //   val packed = SampleGraph.withTimesAndAnyTimes.pack()

    //   for(i <- 1 to 10) {
    //     val v = packed.vertexAt(Location(i.toDouble,1.0))
    //     packed.foreachTransitEdge(v,50) { (t,w) =>
    //       val waitTime =  i*10 - 50
    //       if(waitTime < 0) {
    //         //Should be the AnyTime
    //         w should be (20)
    //       } else {
    //         if(waitTime + i < 20) {
    //           w should be (waitTime + i)
    //         } else { 
    //           w should be (20) 
    //         }
    //       }
    //     }
    //   }
    // }
  }                           
}
