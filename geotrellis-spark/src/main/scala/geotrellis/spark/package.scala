package geotrellis
import geotrellis.spark.formats.ArgWritable
import geotrellis.spark.formats.TileIdWritable
import geotrellis.spark.rdd.SaveImageFunctions

import org.apache.spark.rdd.RDD

import scala.reflect.ClassTag

package object spark {
  implicit def rddToSaveImageFunctions[K <: TileIdWritable: ClassTag, V <: ArgWritable: ClassTag](
      rdd: RDD[(K, V)]) =
    new SaveImageFunctions(rdd)
}