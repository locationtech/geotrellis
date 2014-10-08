package geotrellis.spark

trait KeyProjeciton[K, T] {
  def project(key: K): T 
  def project(t: T): Seq[K]
}

SpatialKeyProjection[K] extends KeyProjection[K, SpatialKey]

TimeKeyProjection[K] extends KeyProjection[K, Double] // Change Double to LocalDateTime

BandKeyProjection[K] extends KeyProjection[K, Int]

ZoomKeyProjection[K] extends KeyProjection[K, Int]





trait TileSpace {
  def transform(spatialKey: SpatialKey): GridCoord
}

trait SpatialComponent {
  def spatialKey: SpatialKey
}

trait TimeComponent {
  def time: Double
}

trait ZoomComponent {
  def zoom: Int
}

trait TileKey

trait  extends TileKey {
  val x: Int
  val y: Int
  val z: Int
}

trait TimeDimesion extends TileKey {
  val time: Double
}
