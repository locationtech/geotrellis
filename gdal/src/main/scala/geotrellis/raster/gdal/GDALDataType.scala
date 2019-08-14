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

import com.azavea.gdal.GDALWarp

object GDALDataType {
  val types =
    List(
      UnknownType,
      TypeByte,
      TypeUInt16,
      TypeInt16,
      TypeUInt32,
      TypeInt32,
      TypeFloat32,
      TypeFloat64,
      TypeCInt16,
      TypeCInt32,
      TypeCFloat32,
      TypeCFloat64
    )

  implicit def intToGDALDataType(i: Int): GDALDataType =
    types.find(_.code == i) match {
      case Some(dt) => dt
      case None => sys.error(s"Invalid GDAL data type code: $i")
    }

  implicit def GDALDataTypeToInt(typ: GDALDataType): Int =
    typ.code
}

abstract sealed class GDALDataType(val code: Int) {
  override
  def toString: String = code.toString
}

// https://github.com/geotrellis/gdal-warp-bindings/blob/9d75e7c65c4c8a0c2c39175a75656bba458a46f0/src/main/java/com/azavea/gdal/GDALWarp.java#L26-L38
case object UnknownType extends GDALDataType(GDALWarp.GDT_Unknown)
case object TypeByte extends GDALDataType(GDALWarp.GDT_Byte)
case object TypeUInt16 extends GDALDataType(GDALWarp.GDT_UInt16)
case object TypeInt16 extends GDALDataType(GDALWarp.GDT_Int16)
case object TypeUInt32 extends GDALDataType(GDALWarp.GDT_UInt32)
case object TypeInt32 extends GDALDataType(GDALWarp.GDT_Int32)
case object TypeFloat32 extends GDALDataType(GDALWarp.GDT_Float32)
case object TypeFloat64 extends GDALDataType(GDALWarp.GDT_Float64)
case object TypeCInt16 extends GDALDataType(GDALWarp.GDT_CInt16)
case object TypeCInt32 extends GDALDataType(GDALWarp.GDT_CInt32)
case object TypeCFloat32 extends GDALDataType(GDALWarp.GDT_CFloat32)
case object TypeCFloat64 extends GDALDataType(GDALWarp.GDT_CFloat64)
