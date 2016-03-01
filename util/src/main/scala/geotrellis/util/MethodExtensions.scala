package geotrellis.util

trait MethodExtensions[+T] extends Serializable {
  def self: T
}
