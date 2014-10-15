package geotrellis.spark

trait KeyProjeciton[K, T] {
  def project(key: K): T 
  def project(t: T): Seq[K]
}

trait SpatialKeyProjection[K] extends KeyProjection[K, SpatialKey]

trait TimeKeyProjection[K] extends KeyProjection[K, Double] // Change Double to LocalDateTime

trait BandKeyProjection[K] extends KeyProjection[K, Int]

trait ZoomKeyProjection[K] extends KeyProjection[K, Int]



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

