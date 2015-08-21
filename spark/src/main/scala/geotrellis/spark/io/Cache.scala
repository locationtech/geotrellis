package geotrellis.spark.io

trait Cache[K, V] {
  def contains(key: K): Boolean
  def apply(key: K): Option[V]
  def update(key: K, value: V): Unit
}
