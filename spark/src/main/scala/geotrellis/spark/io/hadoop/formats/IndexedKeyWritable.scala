package geotrellis.spark.io.hadoop.formats

trait IndexedKeyWritable[K] extends Indexed {
  def key: K
}

trait Indexed {
  def index: Long
  def setIndex(index: Long): Unit
}
