package geotrellis.network.graph

import geotrellis.network._

sealed abstract class EdgeType

case object WalkEdge extends EdgeType {
  def apply(target:Vertex,travelTime:Duration) = 
    Edge(target,Time.ANY,travelTime,WalkEdge)
}

case object TransitEdge extends EdgeType {
  def apply(target:Vertex,time:Time,travelTime:Duration) = 
    Edge(target,time,travelTime,TransitEdge)
}

case object BikeEdge extends EdgeType {
  def apply(target:Vertex,travelTime:Duration) = 
    Edge(target,Time.ANY,travelTime,BikeEdge)
}

case class Edge(target:Vertex,time:Time,travelTime:Duration,edgeType:EdgeType) {
  def isAnyTime = time.isAny
}
