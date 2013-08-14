package geotrellis.network

class Time(private val secondsFromMidnight:Int) extends Serializable {
  def toInt = secondsFromMidnight

  def >(other:Time) = {
    this.toInt > other.toInt
  }

  def <(other:Time) = {
    this.toInt < other.toInt
  }

  def -(other:Time) = {
    new Duration(this.toInt - other.toInt)
  }

  def isAny = 
    secondsFromMidnight == -2

  override
  def toString() = {
    val h = secondsFromMidnight / (60*60)
    val m = (secondsFromMidnight - (h*60*60)) / 60
    val s = secondsFromMidnight - (h*60*60) - (m*60)
    s"Time($h:$m:$s)"
  }

  override 
  def hashCode = secondsFromMidnight.hashCode

  override 
  def equals(other: Any) = 
    other match { 
      case that: Time => this.secondsFromMidnight == that.secondsFromMidnight
      case _ => false 
    }
}

object Time {
  val ANY = new Time(-2)

  def apply(secondsFromMidnight:Int) = new Time(secondsFromMidnight)

  /**
   * Parses a time string in an HH:MM:SS 24-hour format
   */
  def parse(s:String) = {
    val arr = s.split(":")
    if(arr.length != 3) { sys.error("Illegal time format: %s. Must be HH:MM:SS (24 hour)") }
    val hours = 
      try {
        arr(0).toInt
      } catch { case _:java.lang.NumberFormatException => 
          sys.error("Illegal time format: %s. Must be HH:MM:SS (24 hour)")
      }

    val minutes = 
      try {
        arr(1).toInt
      } catch { case _:java.lang.NumberFormatException => 
          sys.error("Illegal time format: %s. Must be HH:MM:SS (24 hour)")
      }

    val seconds = 
      try {
        arr(2).toInt
      } catch { case _:java.lang.NumberFormatException => 
          sys.error("Illegal time format: %s. Must be HH:MM:SS (24 hour)")
      }

    new Time(hours*60*60 + minutes*60 + seconds)
  }
}

class Duration(private val seconds:Int) 
    extends Serializable 
       with Ordered[Duration] {
  def toInt = seconds

  override
  def toString() = {
    if(seconds < 0) { s"Duration(UNREACHABLE)" }
    else { s"Duration($seconds seconds)" }
  }

  override 
  def hashCode = seconds.hashCode

  override 
  def equals(other: Any) = 
    other match { 
      case that: Duration => this.seconds == that.seconds
      case _ => false 
    }

  def isReachable():Boolean = 
    seconds >= 0

  //Ordered
  def compare(other:Duration) = seconds.compare(other.seconds)
}

object Duration {
  val UNREACHABLE = new Duration(-1)

  def apply(seconds:Int) = 
    if(seconds < 0) {
      Duration.UNREACHABLE
    } else {
      new Duration(seconds)
    }

  def fromMinutes(min:Int) = new Duration(min*60)
}

sealed abstract class DayOfWeek extends Serializable

sealed abstract class WeekDay extends DayOfWeek
sealed abstract class Weekend extends DayOfWeek

case object Monday extends WeekDay { override def toString = "Monday" }
case object Tuesday extends WeekDay { override def toString = "Tuesday" }
case object Wednesday extends WeekDay { override def toString = "Wednesday" }
case object Thursday extends WeekDay { override def toString = "Thursday" }
case object Friday extends WeekDay { override def toString = "Friday" }
case object Saturday extends Weekend { override def toString = "Saturday" }
case object Sunday extends Weekend { override def toString = "Sunday" }

trait WeeklySchedule extends Serializable 
{ def isFor(d:DayOfWeek):Boolean }

object EveryDaySchedule extends WeeklySchedule { 
  def isFor(d:DayOfWeek) = true 

  override 
  def toString = "EVERYDAY"
}

object WeekDaySchedule extends WeeklySchedule {
  def isFor(d:DayOfWeek) = 
    d match {
      case _:WeekDay => true
      case _ => false
    }

  override 
  def toString = "WEEKDAYS"
}

case class DaySchedule(target:DayOfWeek) extends WeeklySchedule {
  def isFor(d:DayOfWeek) = d == target

  override def equals(o: Any) = 
    o match {
      case that: DaySchedule => target.equals(that.target)
      case _ => false
    }

  override def hashCode = target.hashCode

  override 
  def toString = s"SINGELDAY($target)"
}
