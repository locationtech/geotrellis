package geotrellis.spark.io.slippy

trait ByteReader[T] extends Serializable {
  def read(bytes: Array[Byte]): T
}
