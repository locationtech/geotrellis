package geotrellis.spark.ingest

import geotrellis.raster.mosaic.MergeView
import org.apache.spark.rdd.RDD

class MergeMethods[K, TileType: MergeView](rdd: RDD[(K, TileType)]){
  def merge(other: RDD[(K, TileType)]): RDD[(K, TileType)] = {
    rdd union other reduceByKey { (tile1, tile2) => tile1 merge tile2 }
  }
}