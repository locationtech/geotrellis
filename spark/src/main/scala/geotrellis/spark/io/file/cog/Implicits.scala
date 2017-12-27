package geotrellis.spark.io.file.cog

import geotrellis.raster._
import geotrellis.spark.io.cog._

trait Implicits extends Serializable {
  implicit val fileSinglebandCOGRDDReader: FileCOGRDDReader[Tile] =
    new FileCOGRDDReader[Tile]

  implicit val fileMultibandCOGRDDReader: FileCOGRDDReader[MultibandTile] =
    new FileCOGRDDReader[MultibandTile]
}
