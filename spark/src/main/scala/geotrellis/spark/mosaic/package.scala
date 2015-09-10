package geotrellis.spark

import geotrellis.raster.mosaic._
import geotrellis.spark.ingest.CellGridPrototypeView
import geotrellis.spark.tiling.LayoutDefinition
import org.apache.spark.rdd.RDD

import scala.reflect.ClassTag

package object mosaic {
  implicit class withRddMergeMethods[K: ClassTag, TileType: MergeView: ClassTag](rdd: RDD[(K, TileType)])
    extends RddMergeMethods[K, TileType](rdd)

  implicit class withRddLayoutMergeMethods[K: SpatialComponent: ClassTag, TileType: MergeView: CellGridPrototypeView: ClassTag](rdd: (RDD[(K, TileType)], LayoutDefinition))
    extends RddLayoutMergeMethods[K, TileType](rdd)

  implicit class withRasterRddMergeMethods[K: SpatialComponent: ClassTag](rdd: RasterRDD[K])
    extends RasterRddMergeMethods[K](rdd)

  implicit class withMultiBandRasterRddMergeMethods[K: SpatialComponent: ClassTag](rdd: MultiBandRasterRDD[K])
    extends MultiBandRasterRddMergeMethods[K](rdd)
}
