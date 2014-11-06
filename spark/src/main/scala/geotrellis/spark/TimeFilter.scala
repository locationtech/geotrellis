package geotrellis.spark

case class TimeFilter[K: TemporalComponent](startTime: Double, endTime: Double) extends KeyFilter[K] {
  def includeKey(key: K): Boolean = {
    val TemporalKey(time) = key.temporalComponent
    startTime <= time && time <= endTime
  }

  def includePartition(minKey: KeyBound[K], maxKey: KeyBound[K]): Boolean = {
    val minTime = 
      minKey match {
        case _: MinKeyBound[K] => Double.MinValue
        case _: MaxKeyBound[K] => Double.MaxValue
        case ValueKeyBound(value) =>
          value.temporalComponent.time
      }

    val maxTime = 
      maxKey match {
        case _: MinKeyBound[K] => Double.MinValue
        case _: MaxKeyBound[K] => Double.MaxValue
        case ValueKeyBound(value) =>
          value.temporalComponent.time
      }

    !(endTime < minTime || maxTime < startTime)
  }
}

object TimeFilter {
  def apply[K: TemporalComponent](time: Double): TimeFilter[K] =
    TimeFilter(time, time)
}
