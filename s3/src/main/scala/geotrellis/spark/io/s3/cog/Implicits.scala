package geotrellis.spark.io.s3.cog

import geotrellis.raster._

trait Implicits extends Serializable {
  implicit val s3SinglebandCOGRDDReader: S3COGRDDReader[Tile] =
    new S3COGRDDReader[Tile]

  implicit val s3SinglebandCOGCollectionReader: S3COGCollectionReader[Tile] =
    new S3COGCollectionReader[Tile]

  implicit val s3MultibandCOGRDDReader: S3COGRDDReader[MultibandTile] =
    new S3COGRDDReader[MultibandTile]

  implicit val s3MultibandCOGCollectionReader: S3COGCollectionReader[MultibandTile] =
    new S3COGCollectionReader[MultibandTile]
}
