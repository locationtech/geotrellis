/*
 * Copyright 2018 Azavea
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
  trait ReadType
  trait S3ReadType { self: PipelineExprType => val `type`: String = "read.s3" }
  case object TemporalS3Type extends ReadType with SinglebandTemporalExprType with S3ReadType
  case object SpatialS3Type extends ReadType with SinglebandSpatialExprType with S3ReadType
  case object MultibandTemporalS3Type extends ReadType with MultibandTemporalExprType with S3ReadType
  case object MultibandSpatialS3Type extends ReadType with MultibandSpatialExprType with S3ReadType

  trait HadoopReadType { self: PipelineExprType => val `type`: String = "read.hadoop" }
  case object TemporalHadoopType extends ReadType with SinglebandTemporalExprType with HadoopReadType
  case object SpatialHadoopType extends ReadType with SinglebandSpatialExprType with HadoopReadType
  case object MultibandTemporalHadoopType extends ReadType with MultibandTemporalExprType with HadoopReadType
  case object MultibandSpatialHadoopType extends ReadType with MultibandSpatialExprType with HadoopReadType

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
  trait TransformType
  trait PerTileReprojectType { self: PipelineExprType => val `type`: String = "transform.per-tile-reproject" }
  case object TemporalPerTileReprojectType extends TransformType with SinglebandTemporalExprType with PerTileReprojectType
  case object SpatialPerTileReprojectType extends TransformType with SinglebandSpatialExprType with PerTileReprojectType
  case object MultibandTemporalPerTileReprojectType extends TransformType with MultibandTemporalExprType with PerTileReprojectType
  case object MultibandSpatialPerTileReprojectType extends TransformType with MultibandSpatialExprType with PerTileReprojectType

  trait BufferedReprojectType { self: PipelineExprType => val `type`: String = "transform.buffered-reproject" }
  case object TemporalBufferedReprojectType extends TransformType with SinglebandTemporalExprType with BufferedReprojectType
  case object SpatialBufferedReprojectType extends TransformType with SinglebandSpatialExprType with BufferedReprojectType
  case object MultibandTemporalBufferedReprojectType extends TransformType with MultibandTemporalExprType with BufferedReprojectType
  case object MultibandSpatialBufferedReprojectType extends TransformType with MultibandSpatialExprType with BufferedReprojectType

  trait TileToLayoutType { self: PipelineExprType => val `type`: String = "transform.tile-to-layout" }
  case object TemporalTileToLayoutType extends TransformType with SinglebandTemporalExprType with TileToLayoutType
  case object SpatialTileToLayoutType extends TransformType with SinglebandSpatialExprType with TileToLayoutType
  case object MultibandTemporalTileToLayoutType extends TransformType with MultibandTemporalExprType with TileToLayoutType
  case object MultibandSpatialTileToLayoutType extends TransformType with MultibandSpatialExprType with TileToLayoutType

  trait RetileToLayoutType { self: PipelineExprType => val `type`: String = "transform.retile-to-layout" }
  case object TemporalRetileToLayoutType extends TransformType with SinglebandTemporalExprType with RetileToLayoutType
  case object SpatialRetileToLayoutType extends TransformType with SinglebandSpatialExprType with RetileToLayoutType
  case object MultibandTemporalRetileToLayoutType extends TransformType with MultibandTemporalExprType with RetileToLayoutType
  case object MultibandSpatialRetileToLayoutType extends TransformType with MultibandSpatialExprType with RetileToLayoutType

  trait PyramidType { self: PipelineExprType => val `type`: String = "transform.pyramid" }
  case object TemporalPyramidType extends TransformType with SinglebandTemporalExprType with PyramidType
  case object SpatialPyramidType extends TransformType with SinglebandSpatialExprType with PyramidType
  case object MultibandTemporalPyramidType extends TransformType with MultibandTemporalExprType with PyramidType
  case object MultibandSpatialPyramidType extends TransformType with MultibandSpatialExprType with PyramidType

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
  trait WriteType { sef: PipelineExprType => val `type`: String = "write" }
  case object TemporalType extends SinglebandTemporalExprType with WriteType
  case object SpatialType extends SinglebandSpatialExprType with WriteType
  case object MultibandTemporalType extends MultibandTemporalExprType with WriteType
  case object MultibandSpatialType extends MultibandSpatialExprType with WriteType

  def fromName(name: String) = name match {
    case TemporalType.getName => TemporalType
    case SpatialType.getName => SpatialType
    case MultibandTemporalType.getName => MultibandTemporalType
    case MultibandSpatialType.getName => MultibandSpatialType

    case _ =>
      throw new UnsupportedOperationException(
        s"Unsupported ReadType $name, pls provide a different fromName function."
      )
  }
}
