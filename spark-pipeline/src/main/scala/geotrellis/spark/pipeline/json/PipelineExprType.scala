package geotrellis.spark.pipeline.json

import scala.util.Try

trait PipelineExprType {
  def name: String

  lazy val getName = name
  override def toString = name
}

object PipelineExprType {
  def fromName(name: String): PipelineExprType =
    Try(ReadTypes.fromName(name)).getOrElse(Try(TransformTypes.fromName(name)).getOrElse(WriteTypes.fromName(name)))
}

trait TemporalExprType extends PipelineExprType {
  val `type`: String
  def name = s"temporal.${`type`}"
}

trait SpatialExprType extends PipelineExprType {
  val `type`: String
  def name = s"spatial.${`type`}"
}

trait SinglebandTemporalExprType extends TemporalExprType {
  val `type`: String
  override def name = s"singleband.${super.name}"
}

trait SinglebandSpatialExprType extends SpatialExprType {
  val `type`: String
  override def name = s"singleband.${super.name}"
}

trait MultibandTemporalExprType extends TemporalExprType {
  val `type`: String
  override def name = s"multiband.${super.name}"
}

trait MultibandSpatialExprType extends SpatialExprType {
  val `type`: String
  override def name = s"multiband.${super.name}"
}

object ReadTypes {
  trait S3ReadType { self: PipelineExprType => val `type`: String = "read.s3" }
  case object TemporalS3Type extends SinglebandTemporalExprType with S3ReadType
  case object SpatialS3Type extends SinglebandSpatialExprType with S3ReadType
  case object MultibandTemporalS3Type extends MultibandTemporalExprType with S3ReadType
  case object MultibandSpatialS3Type extends MultibandSpatialExprType with S3ReadType

  trait HadoopReadType { self: PipelineExprType => val `type`: String = "read.hadoop" }
  case object TemporalHadoopType extends SinglebandTemporalExprType with HadoopReadType
  case object SpatialHadoopType extends SinglebandSpatialExprType with HadoopReadType
  case object MultibandTemporalHadoopType extends MultibandTemporalExprType with HadoopReadType
  case object MultibandSpatialHadoopType extends MultibandSpatialExprType with HadoopReadType

  def fromName(name: String) = name match {
    case TemporalS3Type.getName => TemporalS3Type
    case SpatialS3Type.getName => SpatialS3Type
    case MultibandTemporalS3Type.getName => MultibandTemporalS3Type
    case MultibandSpatialS3Type.getName => MultibandSpatialS3Type

    case TemporalHadoopType.getName => TemporalHadoopType
    case SpatialHadoopType.getName => SpatialHadoopType
    case MultibandTemporalHadoopType.getName => MultibandTemporalHadoopType
    case MultibandSpatialHadoopType.getName => MultibandSpatialHadoopType

    case _ =>
      throw new UnsupportedOperationException(
        s"Unsupported ReadType $name, pls provide a different fromName function."
      )
  }
}

object TransformTypes {
  trait PerTileReprojectType { self: PipelineExprType => val `type`: String = "transform.per-tile-reproject" }
  case object TemporalPerTileReprojectType extends SinglebandTemporalExprType with PerTileReprojectType
  case object SpatialPerTileReprojectType extends SinglebandSpatialExprType with PerTileReprojectType
  case object MultibandTemporalPerTileReprojectType extends MultibandTemporalExprType with PerTileReprojectType
  case object MultibandSpatialPerTileReprojectType extends MultibandSpatialExprType with PerTileReprojectType

  trait BufferedReprojectType { self: PipelineExprType => val `type`: String = "transform.buffered-reproject" }
  case object TemporalBufferedReprojectType extends SinglebandTemporalExprType with BufferedReprojectType
  case object SpatialBufferedReprojectType extends SinglebandSpatialExprType with BufferedReprojectType
  case object MultibandTemporalBufferedReprojectType extends MultibandTemporalExprType with BufferedReprojectType
  case object MultibandSpatialBufferedReprojectType extends MultibandSpatialExprType with BufferedReprojectType

  trait TileToLayoutType { self: PipelineExprType => val `type`: String = "transform.tile-to-layout" }
  case object TemporalTileToLayoutType extends SinglebandTemporalExprType with TileToLayoutType
  case object SpatialTileToLayoutType extends SinglebandSpatialExprType with TileToLayoutType
  case object MultibandTemporalTileToLayoutType extends MultibandTemporalExprType with TileToLayoutType
  case object MultibandSpatialTileToLayoutType extends MultibandSpatialExprType with TileToLayoutType

  trait RetileToLayoutType { self: PipelineExprType => val `type`: String = "transform.retile-to-layout" }
  case object TemporalRetileToLayoutType extends SinglebandTemporalExprType with RetileToLayoutType
  case object SpatialRetileToLayoutType extends SinglebandSpatialExprType with RetileToLayoutType
  case object MultibandTemporalRetileToLayoutType extends MultibandTemporalExprType with RetileToLayoutType
  case object MultibandSpatialRetileToLayoutType extends MultibandSpatialExprType with RetileToLayoutType

  trait PyramidType { self: PipelineExprType => val `type`: String = "transform.pyramid" }
  case object TemporalPyramidType extends SinglebandTemporalExprType with PyramidType
  case object SpatialPyramidType extends SinglebandSpatialExprType with PyramidType
  case object MultibandTemporalPyramidType extends MultibandTemporalExprType with PyramidType
  case object MultibandSpatialPyramidType extends MultibandSpatialExprType with PyramidType

  def fromName(name: String) = name match {
    case TemporalPerTileReprojectType.getName => TemporalPerTileReprojectType
    case SpatialPerTileReprojectType.getName => SpatialPerTileReprojectType
    case MultibandTemporalPerTileReprojectType.getName => MultibandTemporalPerTileReprojectType
    case MultibandSpatialPerTileReprojectType.getName => MultibandSpatialPerTileReprojectType

    case TemporalBufferedReprojectType.getName => TemporalBufferedReprojectType
    case SpatialBufferedReprojectType.getName => SpatialBufferedReprojectType
    case MultibandTemporalBufferedReprojectType.getName => MultibandTemporalBufferedReprojectType
    case MultibandSpatialBufferedReprojectType.getName => MultibandSpatialBufferedReprojectType

    case TemporalTileToLayoutType.getName => TemporalTileToLayoutType
    case SpatialTileToLayoutType.getName => SpatialTileToLayoutType
    case MultibandTemporalTileToLayoutType.getName => MultibandTemporalTileToLayoutType
    case MultibandSpatialTileToLayoutType.getName => MultibandSpatialTileToLayoutType

    case TemporalRetileToLayoutType.getName => TemporalRetileToLayoutType
    case SpatialRetileToLayoutType.getName => SpatialRetileToLayoutType
    case MultibandTemporalRetileToLayoutType.getName => MultibandTemporalRetileToLayoutType
    case MultibandSpatialRetileToLayoutType.getName => MultibandSpatialRetileToLayoutType

    case TemporalPyramidType.getName => TemporalPyramidType
    case SpatialPyramidType.getName => SpatialPyramidType
    case MultibandTemporalPyramidType.getName => MultibandTemporalPyramidType
    case MultibandSpatialPyramidType.getName => MultibandSpatialPyramidType

    case _ =>
      throw new UnsupportedOperationException(
        s"Unsupported ReadType $name, pls provide a different fromName function."
      )
  }
}

object WriteTypes {
  trait FileType { sef: PipelineExprType => val `type`: String = "write.file" }
  case object TemporalFileType extends SinglebandTemporalExprType with FileType
  case object SpatialFileType extends SinglebandSpatialExprType with FileType
  case object MultibandTemporalFileType extends MultibandTemporalExprType with FileType
  case object MultibandSpatialFileType extends MultibandSpatialExprType with FileType

  trait HadoopType { sef: PipelineExprType => val `type`: String = "write.hadoop" }
  case object TemporalHadoopType extends SinglebandTemporalExprType with HadoopType
  case object SpatialHadoopType extends SinglebandSpatialExprType with HadoopType
  case object MultibandTemporalHadoopType extends MultibandTemporalExprType with HadoopType
  case object MultibandSpatialHadoopType extends MultibandSpatialExprType with HadoopType

  def fromName(name: String) = name match {
    case TemporalFileType.getName => TemporalFileType
    case SpatialFileType.getName => SpatialFileType
    case MultibandTemporalFileType.getName => MultibandTemporalFileType
    case MultibandSpatialFileType.getName => MultibandSpatialFileType

    case TemporalHadoopType.getName => TemporalHadoopType
    case SpatialHadoopType.getName => SpatialHadoopType
    case MultibandTemporalHadoopType.getName => MultibandTemporalHadoopType
    case MultibandSpatialHadoopType.getName => MultibandSpatialHadoopType

    case _ =>
      throw new UnsupportedOperationException(
        s"Unsupported ReadType $name, pls provide a different fromName function."
      )
  }
}
