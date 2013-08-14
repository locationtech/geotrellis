package geotrellis.network

/**
 * Object for holding walk time logic.
 */
object Speeds {
  val walking = 1.33 // m/s
  val biking = 4.9  // m/s

  // def walkDuration(start:Location,end:Location):Duration = {
  //   val d = Distance.distance(start,end)
  //   Duration(math.ceil(d/WALKING_SPEED).toInt)
  // }

  // def walkDistance(duration:Duration) =
  //   duration.toInt / WALKING_SPEED
}
