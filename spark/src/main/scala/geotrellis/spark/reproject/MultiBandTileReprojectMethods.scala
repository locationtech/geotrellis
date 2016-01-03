package geotrellis.spark.reproject

import geotrellis.raster._
import geotrellis.proj4._
import geotrellis.spark._
import geotrellis.spark.tiling._

import org.apache.spark.rdd._

import scala.reflect.ClassTag

// class MultiBandTileReprojectMethods[K: SpatialComponent: ClassTag](val self: MultiBandRasterRDD[K]) extends MethodExtensions[MultiBandRasterRDD[K]] {
//   import geotrellis.raster.reproject.Reproject.Options

//   def reproject(destCrs: CRS, layoutScheme: LayoutScheme, options: Options): (Int, MultiBandRasterRDD[K]) =
//     MultiBandTileReproject(self, destCrs, layoutScheme, options)

//   def reproject(destCrs: CRS, layoutScheme: LayoutScheme): (Int, MultiBandRasterRDD[K]) =
//     reproject(destCrs, layoutScheme, Options.DEFAULT)

//   def reproject(destCrs: CRS, layoutScheme: LayoutScheme, bufferSize: Int, options: Options): (Int, MultiBandRasterRDD[K]) =
//     MultiBandTileReproject(self, destCrs, layoutScheme, bufferSize, options)

//   def reproject(destCrs: CRS, layoutScheme: LayoutScheme, bufferSize: Int): (Int, MultiBandRasterRDD[K]) =
//     reproject(destCrs, layoutScheme, bufferSize, Options.DEFAULT)
// }
