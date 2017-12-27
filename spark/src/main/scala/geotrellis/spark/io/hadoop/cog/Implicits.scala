package geotrellis.spark.io.hadoop.cog

import geotrellis.raster._
import geotrellis.spark.io.cog._

trait Implicits extends Serializable {
  implicit val hadoopSinglebandCOGRDDReader: HadoopCOGRDDReader[Tile] =
    new HadoopCOGRDDReader[Tile]

  implicit val hadoopMultibandCOGRDDReader: HadoopCOGRDDReader[MultibandTile] =
    new HadoopCOGRDDReader[MultibandTile]
}
