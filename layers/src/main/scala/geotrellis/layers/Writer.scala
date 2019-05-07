package geotrellis.layers

trait Writer[K, V] extends ((K,V) => Unit) {
  def write(key: K, value: V): Unit
  def apply(key: K, value: V): Unit = write(key, value)
}
