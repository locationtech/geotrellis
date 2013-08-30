package geotrellis.network.graph

import geotrellis.network._

sealed abstract class EdgeType

case object WalkEdge extends EdgeType {
  def apply(target:Vertex,travelTime:Duration) = 
    Edge(target,Time.ANY,travelTime,Walking)
}

case object TransitEdge extends EdgeType {
  def apply(target:Vertex,
            service:String,
            time:Time,
            travelTime:Duration,
            weeklySchedule:WeeklySchedule = EveryDaySchedule) =
    Edge(target,time,travelTime,ScheduledTransit(service,weeklySchedule))
}

case object BikeEdge extends EdgeType {
  def apply(target:Vertex,travelTime:Duration) = 
    Edge(target,Time.ANY,travelTime,Biking)
}

case class Edge(target:Vertex,
                time:Time,
                travelTime:Duration,
                mode:TransitMode) {
  def isAnyTime = time.isAny
}
