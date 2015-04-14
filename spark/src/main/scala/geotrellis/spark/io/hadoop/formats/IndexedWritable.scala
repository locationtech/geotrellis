package geotrellis.spark.io.hadoop.formats

import org.apache.hadoop.io._

trait IndexedKeyWritable[K] { 
  def index: Long 
  def key: K
}
