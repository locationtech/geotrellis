package geotrellis.spark

import geotrellis.proj4.CRS
import geotrellis.raster._
import geotrellis.spark.ingest.IngestKey
import org.apache.spark.rdd.RDD

package object reproject {
  implicit class ReprojectWrapper[T: IngestKey](rdd: RDD[(T, Tile)]) {
    def reproject(destCRS: CRS): RDD[(T, Tile)] = Reproject(rdd, destCRS)
  }

  implicit class ReprojectMultiBandWrapper[T: IngestKey](rdd: RDD[(T, MultiBandTile)]) {
    def reproject(destCRS: CRS): RDD[(T, MultiBandTile)] = MultiBandReproject(rdd, destCRS)
  }
}
