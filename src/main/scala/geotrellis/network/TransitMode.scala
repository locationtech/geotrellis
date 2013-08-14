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

case class PublicTransit(weeklySchedule:WeeklySchedule) extends TransitMode {
  val isTimeDependant = true

  override 
  def equals(o: Any) = 
    o match {
      case that: PublicTransit => weeklySchedule.equals(that.weeklySchedule)
      case _ => false
    }

  override 
  def hashCode = weeklySchedule.hashCode

  override
  def toString = s"MODE(PublicTransit($weeklySchedule))"
}

