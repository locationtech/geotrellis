package geotrellis.network.graph

import geotrellis.network._
import geotrellis.testutil._

import org.scalatest.FunSpec
import org.scalatest.matchers.ShouldMatchers

class ShortestPathSpec extends FunSpec
                          with ShouldMatchers {
  describe("ShortestPathTree") {
    it("should agree on shortest path for simple example from wikipedia") {
      val ShortestPathTestCase(graph,source,expected) = ShortestPathGraphs.simple
      
      val sp = ShortestPathTree(source, Time(0), graph)
      for(x <- 1 to 5) {
        val v = graph.vertexAt(Location(x.toDouble,x.toDouble))
        sp.travelTimeTo(v) should be (Duration(expected(Location(x.toDouble,x.toDouble))))
      }
    }

    it("should handle additional node and times for wikipedia example.") {
      val ShortestPathTestCase(graph,source,expected) = ShortestPathGraphs.simple
    }
  }
}
