package geotrellis.network.graph

import geotrellis.network._
import geotrellis.testutil._

import org.scalatest.FunSpec
import org.scalatest.matchers.ShouldMatchers

import scala.collection.mutable

class TransitGraphSpec extends FunSpec
                          with ShouldMatchers {
  describe("TransitGraph") {
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
        packed.getEdgeIterator(ScheduledTransit("test",EveryDaySchedule),EdgeDirection.Outgoing)
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
        packed.getEdgeIterator(ScheduledTransit("test",EveryDaySchedule),EdgeDirection.Outgoing)
              .foreachEdge(v, 101) { (t,w) => c += 1 }
                 c should be (0)
               }

      val v5 = packed.vertexAt(Location(5.0,1.0))
      packed.getEdgeIterator(ScheduledTransit("test",EveryDaySchedule),EdgeDirection.Outgoing)
            .foreachEdge(v5,20) { (t,w) =>
               w should be ((50-20) + 5)
             }

      val v7 = packed.vertexAt(Location(7.0,1.0))
      packed.getEdgeIterator(ScheduledTransit("test",EveryDaySchedule),EdgeDirection.Outgoing)
            .foreachEdge(v7,20) { (t,w) =>
               w should be ((70-20) + 7)
             }
    }
  }

  describe("Getting an edge iterator") {
    // Code coverage of that mess of a conditional

    val vertices:Map[TransitMode,List[Vertex]] = 
      Map(
        Walking -> List(
          StreetVertex(Location(1.0,1.0),"wA"),
          StreetVertex(Location(2.0,1.0),"wB"),
          StreetVertex(Location(3.0,1.0),"wC"),
          StreetVertex(Location(4.0,1.0),"wD"),
          StreetVertex(Location(5.0,1.0),"wE"),
          StreetVertex(Location(6.0,1.0),"wF"),
          StreetVertex(Location(7.0,1.0),"wG")
        ),
        Biking -> List(
          StreetVertex(Location(1.0,2.0),"bA"),
          StreetVertex(Location(2.0,2.0),"bB"),
          StreetVertex(Location(3.0,2.0),"bC"),
          StreetVertex(Location(4.0,2.0),"bD"),
          StreetVertex(Location(5.0,2.0),"bE"),
          StreetVertex(Location(6.0,2.0),"bF"),
          StreetVertex(Location(7.0,2.0),"bG")
        ),
        ScheduledTransit("test",WeekDaySchedule) -> List(
          StreetVertex(Location(1.0,3.0),"tA"),
          StreetVertex(Location(2.0,3.0),"tB"),
          StreetVertex(Location(3.0,3.0),"tC"),
          StreetVertex(Location(4.0,3.0),"tD"),
          StreetVertex(Location(5.0,3.0),"tE"),
          StreetVertex(Location(6.0,3.0),"tF"),
          StreetVertex(Location(7.0,3.0),"tG")
        ),
        ScheduledTransit("test",DaySchedule(Saturday)) -> List(
          StreetVertex(Location(1.0,4.0),"satA"),
          StreetVertex(Location(2.0,4.0),"satB"),
          StreetVertex(Location(3.0,4.0),"satC"),
          StreetVertex(Location(4.0,4.0),"satD"),
          StreetVertex(Location(5.0,4.0),"satE"),
          StreetVertex(Location(6.0,4.0),"satF"),
          StreetVertex(Location(7.0,4.0),"satG")
        ),
        ScheduledTransit("test",DaySchedule(Sunday)) -> List(
          StreetVertex(Location(1.0,5.0),"sunA"),
          StreetVertex(Location(2.0,5.0),"sunB"),
          StreetVertex(Location(3.0,5.0),"sunC"),
          StreetVertex(Location(4.0,5.0),"sunD"),
          StreetVertex(Location(5.0,5.0),"sunE"),
          StreetVertex(Location(6.0,5.0),"sunF"),
          StreetVertex(Location(7.0,5.0),"sunG")
        )
      )

    val mg = MutableGraph()
    vertices.values.map(_.map(mg.addVertex(_)))

    def connect(vs:List[Vertex])(f:Vertex=>Edge) = {
      vs.dropRight(1).zip(vs.drop(1))
        .map { case (v1:Vertex,v2:Vertex) =>
          mg.addEdge(v1,f(v2))
         }
    }

    connect(vertices(Walking))(WalkEdge(_,Duration(600)))
    connect(vertices(Biking))(BikeEdge(_,Duration(60)))
    connect(vertices(ScheduledTransit("test",WeekDaySchedule)))(TransitEdge(_,"test",Time(1000),Duration(30),WeekDaySchedule))
    connect(vertices(ScheduledTransit("test",DaySchedule(Saturday))))(TransitEdge(_,"test",Time(1000),Duration(20),DaySchedule(Saturday)))
    connect(vertices(ScheduledTransit("test",DaySchedule(Sunday))))(TransitEdge(_,"test",Time(1000),Duration(10),DaySchedule(Sunday)))

    val g = mg.pack

    val vertexMap = (0 until g.vertexCount) map { i => (g.vertexFor(i),i) } toMap

    def checkFor(modes:Seq[TransitMode],edgeDirection:EdgeDirection) = {
      val edgeIterator = g.getEdgeIterator(modes,edgeDirection)

      for(mode <- modes) {
        val len = vertices(mode).length
        val is = 
          edgeDirection match {
            case EdgeDirection.Outgoing => 0 until len-1
            case EdgeDirection.Incoming => (1 until len ).reverse
          }

        val time = 
          edgeDirection match {
            case EdgeDirection.Outgoing => 200
            case EdgeDirection.Incoming => 2000
          }

        for(i <- is) {
          var count = 0
          edgeIterator.foreachEdge(vertexMap(vertices(mode)(i)),time) { (t,w) =>
            count += 1
            t should be (vertexMap(vertices(mode)(
              edgeDirection match {
                case EdgeDirection.Outgoing => i+1
                case EdgeDirection.Incoming => i-1
              })))
          }
          count should be (1)
        }
      }
    }

    it("should iterate over edges for only one anytime, outgoing") {
      checkFor(Seq(Walking),EdgeDirection.Outgoing)
    }

    it("should iterate over edges for only one anytime, incoming") {
      checkFor(Seq(Walking),EdgeDirection.Incoming)
    }

    it("should iterate over edges for only multiple anytime, outgoing") {
      checkFor(Seq(Walking,Biking),EdgeDirection.Outgoing)
    }

    it("should iterate over edges for only multiple anytime, incoming") {
      checkFor(Seq(Walking,Biking),EdgeDirection.Incoming)
    }

    it("should iterate over edges for only one transit, outgoing") {
      checkFor(Seq(Walking,ScheduledTransit("test",WeekDaySchedule)),EdgeDirection.Outgoing)
    }

    it("should iterate over edges for only one transit, incoming") {
      checkFor(Seq(Walking,ScheduledTransit("test",WeekDaySchedule)),EdgeDirection.Incoming)
    }

    it("should iterate over edges for multiple transit, outgoing") {
     checkFor(Seq(Walking,ScheduledTransit("test",WeekDaySchedule),ScheduledTransit("test",DaySchedule(Sunday))),
              EdgeDirection.Outgoing)
    }

    it("should iterate over edges for multiple transit, incoming") {
      checkFor(Seq(Walking,ScheduledTransit("test",WeekDaySchedule),ScheduledTransit("test",DaySchedule(Sunday))),
               EdgeDirection.Incoming)
    }

    it("should iterate over edges for one anytime, one transit outgoing") {
      checkFor(Seq(Walking,ScheduledTransit("test",DaySchedule(Sunday))),
               EdgeDirection.Outgoing)
    }

    it("should iterate over edges for one anytime, one transit incoming") {
      checkFor(Seq(Walking,ScheduledTransit("test",DaySchedule(Sunday))),
               EdgeDirection.Incoming)
    }

    it("should iterate over edges for multiple anytime, one transit outgoing") {
      checkFor(Seq(Walking,Biking,ScheduledTransit("test",DaySchedule(Sunday))),
               EdgeDirection.Outgoing)
    }

    it("should iterate over edges for multiple anytime, one transit incoming") {
      checkFor(Seq(Walking,Biking,ScheduledTransit("test",DaySchedule(Sunday))),
               EdgeDirection.Incoming)
    }

    it("should iterate over edges for one anytime, multiple transit outgoing") {
      checkFor(Seq(Walking,ScheduledTransit("test",WeekDaySchedule),ScheduledTransit("test",DaySchedule(Sunday))),
               EdgeDirection.Outgoing)
    }

    it("should iterate over edges for one anytime, multiple transit incoming") {
      checkFor(Seq(Walking,ScheduledTransit("test",WeekDaySchedule),ScheduledTransit("test",DaySchedule(Sunday))),
               EdgeDirection.Incoming)
    }

    it("should iterate over edges for multiple anytime, multiple transit, outgoing") {
      checkFor(Seq(Walking,Biking,ScheduledTransit("test",WeekDaySchedule),ScheduledTransit("test",DaySchedule(Sunday))),
               EdgeDirection.Outgoing)
    }

    it("should iterate over edges for mutliple anytime, multiple transit, incoming") {
      checkFor(Seq(Walking,Biking,ScheduledTransit("test",WeekDaySchedule),ScheduledTransit("test",DaySchedule(Sunday))),
               EdgeDirection.Incoming)
    }
  }
}
