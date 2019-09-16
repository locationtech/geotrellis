/*
 * Copyright 2017 Azavea
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

package geotrellis.raster

import geotrellis.raster.CellTypeEncoding.CellTypeEncoding

import scala.util.Try

/**
 * Enumeration of tokens used to represent the different types of cell encodings and their
 * associated no-data values in GeoTrellis. Used primarily for serialization to/from String, and
 * pattern-matching operations.
 */
object CellTypeEncoding {
  sealed trait CellTypeEncoding {
    val name: String
  }
  case object bool extends FixedNoDataEncoding {
    override val name = "bool"
  }
  case object boolraw extends FixedNoDataEncoding {
    override val name = "boolraw"
  }
  case object int8raw extends FixedNoDataEncoding {
    override val name = "int8raw"
  }
  case object uint8raw extends FixedNoDataEncoding {
    override val name = "uint8raw"
  }
  case object int16raw extends FixedNoDataEncoding {
    override val name = "int16raw"
  }
  case object uint16raw extends FixedNoDataEncoding {
    override val name = "uint16raw"
  }
  case object int32raw extends FixedNoDataEncoding {
    override val name = "int32raw"
  }
  case object float32raw extends FixedNoDataEncoding {
    override val name = "float32raw"
  }
  case object float64raw extends FixedNoDataEncoding {
    override val name = "float64raw"
  }
  case object int8 extends FixedNoDataEncoding {
    override val name = "int8"
  }
  case object uint8 extends FixedNoDataEncoding {
    override val name = "uint8"
  }
  case object int16 extends FixedNoDataEncoding {
    override val name = "int16"
  }
  case object uint16 extends FixedNoDataEncoding {
    override val name = "uint16"
  }
  case object int32 extends FixedNoDataEncoding {
    override val name = "int32"
  }
  case object float32 extends FixedNoDataEncoding {
    override val name = "float32"
  }
  case object float64 extends FixedNoDataEncoding {
    override val name = "float64"
  }
  case object int8ud extends UserDefinedNoDataEncoding {
    override val name = "int8ud"
  }
  case object uint8ud extends UserDefinedNoDataEncoding {
    override val name = "uint8ud"
  }
  case object int16ud extends UserDefinedNoDataEncoding {
    override val name = "int16ud"
  }
  case object uint16ud extends UserDefinedNoDataEncoding {
    override val name = "uint16ud"
  }
  case object int32ud extends UserDefinedNoDataEncoding {
    override val name = "int32ud"
  }
  case object float32ud extends UserDefinedNoDataEncoding {
    override val name = "float32ud"
    override val isFloatingPoint = true
  }
  object float64ud extends UserDefinedNoDataEncoding {
    override val name = "float64ud"
    override val isFloatingPoint = true
  }
}

/** Base trait for encoding CellTypes with fixed or no NoData values. */
sealed trait FixedNoDataEncoding extends CellTypeEncoding {
  def unapplySeq(text: String): Option[Seq[_]] = {
    if(text == name) Some(Seq.empty) else None
  }
}

/** Base trait for encoding CellTypes with user defined NoData values. */
sealed trait UserDefinedNoDataEncoding extends CellTypeEncoding { self ⇒
  val name: String
  val isFloatingPoint = false

  /** Create a cell encoding for a specific noData value. */
  def apply[T](noData: T) = SpecifiedUserDefinedNoDataEncoding(self, noData)

  def unapply(text: String): Option[WidenedNoData] = {
    if (text.startsWith(name)) {
      val number = text.replace(name, "")

      if(isFloatingPoint) Try(number.toDouble).map(WideDoubleNoData).toOption
      else Try(number.toInt).map(WideIntNoData.apply).toOption
    }
    else None
  }
}

/** On-demand cell type encoding for specific user defined no data values. */
case class SpecifiedUserDefinedNoDataEncoding[N](
  base: UserDefinedNoDataEncoding, noData: N) extends UserDefinedNoDataEncoding {
  override val name = base.name + noData
  override def apply[T](noData: T) = SpecifiedUserDefinedNoDataEncoding(base, noData)
}

/** Container for NoData value using wider word size than declared to handled
 * unsigned number rollover.
 */
sealed trait WidenedNoData {
  def asInt: Int
  def asDouble: Double
}
/** NoData value stored as an Int. */
case class WideIntNoData(asInt: Int) extends WidenedNoData {
  def asDouble = asInt.toDouble
  override def toString = asInt.toString
}
object WideIntNoData {
  def apply(thinned: Byte) = new WideIntNoData(thinned & 0xFF)
  def apply(thinned: Short) = new WideIntNoData(thinned & 0xFFFF)
}

/** NoData stored as a Double */
case class WideDoubleNoData(asDouble: Double) extends WidenedNoData {
  def asInt = asDouble.toInt
  override def toString = asDouble.toString
}


