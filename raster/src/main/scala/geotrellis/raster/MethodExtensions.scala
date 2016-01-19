package geotrellis.raster

trait MethodExtensions[+T] extends Serializable {
  type Self = T
  def self: T
}
