package geotrellis.tiling

import java.time.Instant


object Implicits extends Implicits


trait Implicits {
  implicit def longToInstant(millis: Long): Instant = Instant.ofEpochMilli(millis)
}
