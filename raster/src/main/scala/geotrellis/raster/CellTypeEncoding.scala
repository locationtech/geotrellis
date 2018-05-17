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

import scala.util.Try

/**
 * Enumeration of tokens used to represent the different types of cell encodings and their
 * associated no-data values in GeoTrellis. Used primarily for serialization to/from String, and
 * pattern-matching operations.
 */
object CellTypeEncoding {
  case object bool extends FixedNoDataEncoding
  case object boolraw extends FixedNoDataEncoding
  case object int8raw extends FixedNoDataEncoding
  case object uint8raw extends FixedNoDataEncoding
  case object int16raw extends FixedNoDataEncoding
  case object uint16raw extends FixedNoDataEncoding
  case object int32raw extends FixedNoDataEncoding
  case object float32raw extends FixedNoDataEncoding
  case object float64raw extends FixedNoDataEncoding
  case object int8 extends FixedNoDataEncoding
  case object uint8 extends FixedNoDataEncoding
  case object int16 extends FixedNoDataEncoding
  case object uint16 extends FixedNoDataEncoding
  case object int32 extends FixedNoDataEncoding
  case object float32 extends FixedNoDataEncoding
  case object float64 extends FixedNoDataEncoding
  case object int8ud extends UserDefinedNoDataEncoding
  case object uint8ud extends UserDefinedNoDataEncoding
  case object int16ud extends UserDefinedNoDataEncoding
  case object uint16ud extends UserDefinedNoDataEncoding
  case object int32ud extends UserDefinedNoDataEncoding
  case object float32ud extends UserDefinedNoDataEncoding {
    override val isFloatingPoint = true
  }
  object float64ud extends UserDefinedNoDataEncoding {
    override val isFloatingPoint = true
  }
}

/** Root trait for CellType encoding definitions. */
sealed trait CellTypeEncoding {
  def name = getClass.getName.split('$').last
}

/** Base trait for encoding CellTypes with fixed or no NoData values. */
sealed trait FixedNoDataEncoding extends CellTypeEncoding {
  def unapplySeq(text: String): Option[Seq[_]] = {
    if(text == name) Some(Seq.empty) else None
  }
}

/** Base trait for encoding CellTypes with user defined NoData values. */
sealed trait UserDefinedNoDataEncoding extends CellTypeEncoding { self ⇒
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
  override def name = base.name + noData
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


