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
import org.apache.hadoop.fs.Path

package object spark {

  type TileIdRaster = (Long, Raster)
  type TileIdCoordRaster = (Long, Long, Long, Raster)
  type TileIdArgWritable = (TileIdWritable, ArgWritable)

  implicit class SavableRasterWritable(val raster: RDD[TileIdArgWritable]) {
    def save(path: Path) = SaveRasterFunctions.save(raster, path)
  }

  implicit class MakeRasterRDD(val prev: RDD[TileIdRaster]) {
    def withMetadata(meta: PyramidMetadata) = new RasterRDD(prev, meta)
  }
  implicit class SavableRasterRDD(val rdd: RasterRDD) {
    def save(path: Path) = SaveRasterFunctions.save(rdd, path)
  }
}