package geotrellis.spark

import com.github.nscala_time.time.Imports._

case class TimeFilter[K: TemporalComponent](startTime: DateTime, endTime: DateTime) extends KeyFilter[K] {
  def includeKey(key: K): Boolean = {
    val TemporalKey(time) = key.temporalComponent
    startTime <= time && time <= endTime
  }

  def includePartition(minKey: KeyBound[K], maxKey: KeyBound[K]): Boolean = {
    val minTime = 
      minKey match {
        case _: MinKeyBound[K] => new DateTime(Long.MinValue/10, DateTimeZone.UTC) // a very low year
        case _: MaxKeyBound[K] => new DateTime(Long.MaxValue/10, DateTimeZone.UTC) // a vary high year
        case ValueKeyBound(value) =>
          value.temporalComponent.time
      }

    val maxTime = 
      maxKey match {
        case _: MinKeyBound[K] => new DateTime(Long.MinValue/10, DateTimeZone.UTC)
        case _: MaxKeyBound[K] => new DateTime(Long.MaxValue/10, DateTimeZone.UTC)
        case ValueKeyBound(value) =>
          value.temporalComponent.time
      }

    !(endTime < minTime || maxTime < startTime)
  }
}

object TimeFilter {
  def apply[K: TemporalComponent](time: DateTime): TimeFilter[K] =
    TimeFilter(time, time)
}
