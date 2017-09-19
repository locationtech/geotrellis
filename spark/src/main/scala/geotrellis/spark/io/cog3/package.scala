package geotrellis.spark.io

import geotrellis.spark.SpatialKey
import geotrellis.spark.io.cog3.GeoTiffLayerMetadata.IndexRDD
import geotrellis.spark.io.index.KeyIndex

package object cog3 {
  implicit class withKeyIndexRDDPersistent(ip: (KeyIndex[SpatialKey], List[(Int, IndexRDD)])) {
    val keyIndex: KeyIndex[SpatialKey] = ip._1
    val pyramid: List[(Int, IndexRDD)] = ip._2


  }
}
