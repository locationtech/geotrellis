package geotrellis.network

abstract sealed class TransitMode extends Serializable 
{ val isTimeDependant:Boolean }

case object Walking extends TransitMode { 
  val isTimeDependant = false 

  override
  def toString = "MODE(Walking)"
}
case object Biking extends TransitMode { 
  val isTimeDependant = false 

  override
  def toString = "MODE(Biking)"
}

case class ScheduledTransit(service:String,weeklySchedule:WeeklySchedule) extends TransitMode {
  val isTimeDependant = true

  override
  def toString = s"MODE(ScheduledTransit($service,$weeklySchedule))"
}
