package geotrellis.util


/**
  * The base-trait from which all implicit classes containing
  * extension methods are derived.
  */
trait MethodExtensions[+T] extends Serializable {
  def self: T
}
