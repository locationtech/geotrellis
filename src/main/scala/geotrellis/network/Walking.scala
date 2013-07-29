package geotrellis.network

/**
 * Object for holding walk time logic.
 */
object Walking {
  val WALKING_SPEED = 1.33 // m/s

  def walkDuration(start:Location,end:Location):Duration = {
    val d = Distance.distance(start,end)
    Duration(math.ceil(d/WALKING_SPEED).toInt)
  }

  def walkDistance(duration:Duration) =
    duration.toInt / WALKING_SPEED
}
