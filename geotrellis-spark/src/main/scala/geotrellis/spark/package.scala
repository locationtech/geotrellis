package geotrellis

import scala.reflect.ClassTag
import org.apache.hadoop.io.Writable
import org.apache.spark.rdd.RDD
import geotrellis.spark.rdd.SaveImageFunctions
import geotrellis.spark.formats.TileIdWritable
import geotrellis.spark.formats.ArgWritable

package object spark {
  implicit def rddToSaveImageFunctions[K <% TileIdWritable: ClassTag, V <% ArgWritable: ClassTag](
      rdd: RDD[(K, V)]) =
    new SaveImageFunctions(rdd)
}