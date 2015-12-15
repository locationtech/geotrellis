package geotrellis.spark.io

trait Reader[K, V] extends (K => V) {
  def read(key: K): V
  def apply(key: K): V = read(key)
}