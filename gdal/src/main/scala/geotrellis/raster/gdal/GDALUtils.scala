/*
 * Copyright 2019 Azavea
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

package geotrellis.raster.gdal

import geotrellis.raster._
import geotrellis.raster.io.geotiff._
import geotrellis.raster.resample._

object GDALUtils {
  def deriveResampleMethodString(method: ResampleMethod): String =
    method match {
      case NearestNeighbor => "near"
      case Bilinear => "bilinear"
      case CubicConvolution => "cubic"
      case CubicSpline => "cubicspline"
      case Lanczos => "lanczos"
      case Average => "average"
      case Mode => "mode"
      case Max => "max"
      case Min => "min"
      case Median => "med"
      case _ => throw new Exception(s"Could not find equivalent GDALResampleMethod for: $method")
    }

  def dataTypeToCellType(datatype: GDALDataType, noDataValue: Option[Double] = None, typeSizeInBits: => Option[Int] = None, minMaxValues: => Option[(Double, Double)] = None): CellType =
    datatype match {
      case TypeByte =>
        typeSizeInBits match {
          case Some(bits) if bits == 1 => BitCellType
          case _ =>
            minMaxValues match {
              case Some((mi, ma)) if (mi.toInt >= 0 && mi <= 255) && (ma.toInt >= 0 && ma <= 255) =>
                noDataValue match {
                  case Some(nd) if nd.toInt > 0 && nd <= 255 => UByteUserDefinedNoDataCellType(nd.toByte)
                  case Some(nd) if nd.toInt == 0 => UByteConstantNoDataCellType
                  case _ => UByteCellType
                }
              case _ =>
                noDataValue match {
                  case Some(nd) if nd.toInt > Byte.MinValue.toInt && nd <= Byte.MaxValue.toInt => ByteUserDefinedNoDataCellType(nd.toByte)
                  case Some(nd) if nd.toInt == Byte.MinValue.toInt => ByteConstantNoDataCellType
                  case _ => ByteCellType
                }
            }
        }
      case TypeUInt16 =>
        noDataValue match {
          case Some(nd) if nd.toInt > 0 && nd <= 65535 => UShortUserDefinedNoDataCellType(nd.toShort)
          case Some(nd) if nd.toInt == 0 => UShortConstantNoDataCellType
          case _ => UShortCellType
        }
      case TypeInt16 =>
        noDataValue match {
          case Some(nd) if nd > Short.MinValue.toDouble && nd <= Short.MaxValue.toDouble => ShortUserDefinedNoDataCellType(nd.toShort)
          case Some(nd) if nd == Short.MinValue.toDouble => ShortConstantNoDataCellType
          case _ => ShortCellType
        }
      case TypeUInt32 =>
        noDataValue match {
          case Some(nd) if nd.toLong > 0l && nd.toLong <= 4294967295l => FloatUserDefinedNoDataCellType(nd.toFloat)
          case Some(nd) if nd.toLong == 0l => FloatConstantNoDataCellType
          case _ => FloatCellType
        }
      case TypeInt32 =>
        noDataValue match {
          case Some(nd) if nd.toInt > Int.MinValue && nd.toInt <= Int.MaxValue => IntUserDefinedNoDataCellType(nd.toInt)
          case Some(nd) if nd.toInt == Int.MinValue => IntConstantNoDataCellType
          case _ => IntCellType
        }
      case TypeFloat32 =>
        noDataValue match {
          case Some(nd) if isData(nd) && Float.MinValue.toDouble <= nd && Float.MaxValue.toDouble >= nd => FloatUserDefinedNoDataCellType(nd.toFloat)
          case Some(nd) => FloatConstantNoDataCellType
          case _ => FloatCellType
        }
      case TypeFloat64 =>
        noDataValue match {
          case Some(nd) if isData(nd) => DoubleUserDefinedNoDataCellType(nd)
          case Some(nd) => DoubleConstantNoDataCellType
          case _ => DoubleCellType
        }
      case UnknownType =>
        throw new UnsupportedOperationException(s"Datatype ${datatype} is not supported.")
      case TypeCInt16 | TypeCInt32 | TypeCFloat32 | TypeCFloat64 =>
        throw new UnsupportedOperationException("Complex datatypes are not supported.")
    }

  def deriveOverviewStrategyString(strategy: OverviewStrategy): String = strategy match {
    case Auto(n) => s"AUTO-$n"
    case AutoHigherResolution => "AUTO"
    case Base => "NONE"
  }
}
