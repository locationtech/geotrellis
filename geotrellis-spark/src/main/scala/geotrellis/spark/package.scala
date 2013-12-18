package geotrellis

import scala.reflect.ClassTag
import org.apache.hadoop.io.Writable
import org.apache.spark.rdd.RDD
import geotrellis.spark.rdd.SaveImageFunctions
package object spark {
  implicit def rddToSaveImageFunctions[K <% Writable: ClassTag, V <% Writable: ClassTag](
      rdd: RDD[(K, V)]) =
    new SaveImageFunctions(rdd)
}