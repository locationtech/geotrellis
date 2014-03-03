package geotrellis
import geotrellis.spark.formats.ArgWritable
import geotrellis.spark.formats.TileIdWritable
import geotrellis.spark.rdd.SaveRasterFunctions

import org.apache.spark.rdd.RDD

import scala.reflect.ClassTag

package object spark {
  
  type TileId = Long
  
  type RasterWritableRDD = RDD[(TileIdWritable, ArgWritable)]
  //type ImageRDD = RDD[(TileId, RasterData)]

  implicit class SavableImage(val image: RasterWritableRDD) {
    def save(path: String) = SaveRasterFunctions.save(image, path)
  }
}