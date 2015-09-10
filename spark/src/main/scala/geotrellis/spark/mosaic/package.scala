package geotrellis.spark

import geotrellis.raster.mosaic._
import geotrellis.spark.tiling.LayoutDefinition
import org.apache.spark.rdd.RDD

package object mosaic {
  implicit class withRddMergeMethods[K, TileType: MergeView](rdd: RDD[(K, TileType)])
    extends RddMergeMethods[K, TileType](rdd)

  implicit class withRddLayoutMergeMethods[K, TileType: MergeView](rdd: (RDD[(K, TileType)], LayoutDefinition))
    extends RddLayoutMergeMethods[K, TileType](rdd)

  implicit class withRasterRddMergeMethods[K, TileType: MergeView](rdd: RasterRDD[K])
    extends RasterRddMergeMethods[K](rdd)

  implicit class withMultiBandRasterRddMergeMethods[K, TileType: MergeView](rdd: MultiBandRasterRDD[K])
    extends MultiBandRasterRddMergeMethods[K](rdd)
}
