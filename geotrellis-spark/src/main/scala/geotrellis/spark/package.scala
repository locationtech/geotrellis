package geotrellis
import geotrellis.spark.formats.ArgWritable
import geotrellis.spark.formats.TileIdWritable
import geotrellis.spark.metadata.PyramidMetadata
import geotrellis.spark.rdd.RasterRDD
import org.apache.spark.rdd.RDD
import geotrellis.spark.rdd.SaveRasterFunctions
import geotrellis.spark.rdd.SaveRasterFunctions
import geotrellis.spark.formats.ArgWritable
import geotrellis.spark.metadata.PyramidMetadata
import geotrellis.spark.formats.TileIdWritable

package object spark {

  type TileIdRaster = (Long, Raster)
  type TileIdCoordRaster = (Long, Long, Long, Raster)
  type TileIdArgWritable = (TileIdWritable, ArgWritable)
  
  implicit class SavableRasterWritable(val raster: RDD[(TileIdWritable, ArgWritable)]) {
    def save(path: String) = SaveRasterFunctions.save(raster, path)
  }

  implicit class DecoratedRasterRDD(val prev: RDD[(Long, Raster)]) {
    def withMetadata(meta: PyramidMetadata) = new RasterRDD[RDD[TileIdRaster]](prev, meta)
    def save(path: String) = SaveRasterFunctions.save(prev, path)
  } 
}