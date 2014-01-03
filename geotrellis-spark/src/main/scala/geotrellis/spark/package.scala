package geotrellis
import geotrellis.spark.formats.ArgWritable
import geotrellis.spark.formats.TileIdWritable
import geotrellis.spark.rdd.SaveImageFunctions

import org.apache.spark.rdd.RDD

import scala.reflect.ClassTag

package object spark {
  
  type TileId = Long
  
  type ImageWritableRDD = RDD[(TileIdWritable, ArgWritable)]
  //type ImageRDD = RDD[(TileId, RasterData)]

  implicit class SavableImage(val image: ImageWritableRDD) {
    def save(path: String) = SaveImageFunctions.save(image, path)
  }
}