package geotrellis.spark.io.avro

import org.scalatest._
import Matchers._

import geotrellis.raster._
import geotrellis.util.MethodExtensions

trait AvroTools { self: Matchers =>
  import AvroTools._

  def roundTrip[T](thing: T)(implicit codec: AvroRecordCodec[T]): Unit = {
    val bytes = AvroEncoder.toBinary(thing)
    val fromBytes = AvroEncoder.fromBinary[T](bytes)
    fromBytes shouldBe thing
    val json = AvroEncoder.toJson(thing)
    val fromJson = AvroEncoder.fromJson[T](json)
    fromJson shouldBe thing
  }

  def roundTripWithNoDataCheck[T : AvroRecordCodec : (? => AvroNoDataCheckMethods[T])](thing: T): Unit = {
    val bytes = AvroEncoder.toBinary(thing)
    val fromBytes = AvroEncoder.fromBinary[T](bytes)
    fromBytes shouldBe thing
    val json = AvroEncoder.toJson(thing)
    thing.checkNoData(json)
    val fromJson = AvroEncoder.fromJson[T](json)
    fromJson shouldBe thing
  }
}

object AvroTools {
  import scala.util.parsing.json._
  trait AvroNoDataCheckMethods[T] extends MethodExtensions[T] {
    def checkNoData(json: String): Unit
  }
  trait NoDataValueChecker[T] {
    def checkNoData(json: String): Unit = {
      val noDataParsed: Option[Any] = extractNoData(json)
      doCheck(noDataParsed)
    }
    def extractNoData(json: String): Option[Any] = {
      JSON.parseFull(json) flatMap {
        case m: Map[_,_] => m.asInstanceOf[Map[String, Any]].get("noDataValue")
      }
    }
    def doCheck(noData: Option[Any]): Unit = ()
  }
  implicit class ShortNoDataValueCheckMethods(val self: ShortArrayTile) extends
    ShortNoDataChecker(self.cellType) with AvroNoDataCheckMethods[ShortArrayTile] {}

  class ShortNoDataChecker(cellType: CellType) extends NoDataValueChecker[ShortArrayTile] {
    override def doCheck(nodata: Option[Any]): Unit = {
      cellType match {
        case ShortConstantNoDataCellType => nodata shouldBe Some(Map("int" -> shortNODATA.toInt))
        case ShortUserDefinedNoDataCellType(nd) => nodata shouldBe Some(Map("int" -> nd))
        case ShortCellType => nodata shouldBe Some(null)
        case _ => sys.error(s"Cell type ${cellType} was unexpected")
      }
    }
  }

  implicit class UShortNoDataValueCheckMethods(val self: UShortArrayTile) extends
    UShortNoDataChecker(self.cellType) with AvroNoDataCheckMethods[UShortArrayTile] {}

  class UShortNoDataChecker(cellType: CellType) extends NoDataValueChecker[UShortArrayTile] {
    override def doCheck(nodata: Option[Any]): Unit = {
      cellType match {
        case UShortConstantNoDataCellType => nodata shouldBe Some(Map("int" -> ushortNODATA.toInt))
        case UShortUserDefinedNoDataCellType(nd) => nodata shouldBe Some(Map("int" -> nd))
        case UShortCellType => nodata shouldBe Some(null)
        case _ => sys.error(s"Cell type ${cellType} was unexpected")
      }
    }
  }

  implicit class IntNoDataValueCheckMethods(val self: IntArrayTile) extends
    IntNoDataChecker(self.cellType) with AvroNoDataCheckMethods[IntArrayTile] {}

  class IntNoDataChecker(cellType: CellType) extends NoDataValueChecker[IntArrayTile] {
    override def doCheck(nodata: Option[Any]): Unit = {
      cellType match {
        case IntConstantNoDataCellType => nodata shouldBe Some(Map("int" -> NODATA))
        case IntUserDefinedNoDataCellType(nd) => nodata shouldBe Some(Map("int" -> nd))
        case IntCellType => nodata shouldBe Some(null)
        case _ => sys.error(s"Cell type ${cellType} was unexpected")
      }
    }
  }

  implicit class FloatNoDataValueCheckMethods(val self: FloatArrayTile) extends
    FloatNoDataChecker(self.cellType) with AvroNoDataCheckMethods[FloatArrayTile] {}

  class FloatNoDataChecker(cellType: CellType) extends NoDataValueChecker[FloatArrayTile] {
    override def doCheck(nodata: Option[Any]): Unit = {
      cellType match {
        case FloatConstantNoDataCellType => nodata shouldBe Some(Map("boolean" -> true))
        case FloatUserDefinedNoDataCellType(nd) =>
          // nodata shouldBe Some(Map("float" -> nd)) // doesn't work: double number != float number
          // nodata shouldBe Some(Map("float" -> nd.toDouble)) // doesn't work: (2.2f).toDouble ==> 2.200000047683716
          nodata.toString shouldBe Some(Map("float" -> nd)).toString
        case FloatCellType => nodata shouldBe Some(Map("boolean" -> false))
        case _ => sys.error(s"Cell type ${cellType} was unexpected")
      }
    }
  }

  implicit class DoubleNoDataValueCheckMethods(val self: DoubleArrayTile) extends
    DoubleNoDataChecker(self.cellType) with AvroNoDataCheckMethods[DoubleArrayTile] {}

  class DoubleNoDataChecker(cellType: CellType) extends NoDataValueChecker[DoubleArrayTile] {
    override def doCheck(nodata: Option[Any]): Unit = {
      cellType match {
        case DoubleConstantNoDataCellType => nodata shouldBe Some(Map("boolean" -> true))
        case DoubleUserDefinedNoDataCellType(nd) => nodata shouldBe Some(Map("double" -> nd))
        case DoubleCellType => nodata shouldBe Some(Map("boolean" -> false))
        case _ => sys.error(s"Cell type ${cellType} was unexpected")
      }
    }
  }

  implicit class ByteNoDataValueCheckMethods(val self: ByteArrayTile) extends
    ByteNoDataChecker(self.cellType) with AvroNoDataCheckMethods[ByteArrayTile] {}

  class ByteNoDataChecker(cellType: CellType) extends NoDataValueChecker[ByteArrayTile] {
    override def doCheck(nodata: Option[Any]): Unit = {
      cellType match {
        case ByteConstantNoDataCellType => nodata shouldBe Some(Map("int" -> byteNODATA))
        case ByteUserDefinedNoDataCellType(nd) => nodata shouldBe Some(Map("int" -> nd))
        case ByteCellType => nodata shouldBe Some(null)
        case _ => sys.error(s"Cell type ${cellType} was unexpected")
      }
    }
  }

  implicit class UByteNoDataValueCheckMethods(val self: UByteArrayTile) extends
    UByteNoDataChecker(self.cellType) with AvroNoDataCheckMethods[UByteArrayTile] {}

  class UByteNoDataChecker(cellType: CellType) extends NoDataValueChecker[UByteArrayTile] {
    override def doCheck(nodata: Option[Any]): Unit = {
      cellType match {
        case UByteConstantNoDataCellType => nodata shouldBe Some(Map("int" -> ubyteNODATA))
        case UByteUserDefinedNoDataCellType(nd) => nodata shouldBe Some(Map("int" -> nd))
        case UByteCellType => nodata shouldBe Some(null)
        case _ => sys.error(s"Cell type ${cellType} was unexpected")
      }
    }
  }

  implicit class BitNoDataValueCheckMethods(val self: BitArrayTile) extends
    BitNoDataChecker(self.cellType) with AvroNoDataCheckMethods[BitArrayTile] {}

  class BitNoDataChecker(cellType: CellType) extends NoDataValueChecker[BitArrayTile] {
    override def doCheck(nodata: Option[Any]): Unit = {
      nodata shouldBe None
    }
  }

  implicit class MultibandNoDataValueCheckMethods(val self: MultibandTile) extends AvroNoDataCheckMethods[MultibandTile] {
      override def checkNoData(json: String): Unit = {
        JSON.parseFull(json) foreach {
          case m: Map[_,_] =>
            m.asInstanceOf[Map[String, Seq[Map[String, Any]]]].apply("bands") foreach { bandWrapper =>
              val band = bandWrapper(bandWrapper.keys.head).asInstanceOf[Map[String,Any]]

              val nodata = band.get("noDataValue")
              self.cellType match {
                case ct: ShortCells =>   new ShortNoDataChecker(ct).doCheck(nodata)
                case ct: UShortCells =>  new UShortNoDataChecker(ct).doCheck(nodata)
                case ct: IntCells =>     new IntNoDataChecker(ct).doCheck(nodata)
                case ct: FloatCells =>   new FloatNoDataChecker(ct).doCheck(nodata)
                case ct: DoubleCells =>  new DoubleNoDataChecker(ct).doCheck(nodata)
                case ct: ByteCells =>    new ByteNoDataChecker(ct).doCheck(nodata)
                case ct: UByteCells =>   new UByteNoDataChecker(ct).doCheck(nodata)
                case ct: BitCells =>     new BitNoDataChecker(ct).doCheck(nodata)
                case _ => sys.error(s"Cell type ${self.cellType} was unexpected")
              }
            }
        }
      }
    }

}
