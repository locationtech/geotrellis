package geotrellis.spark.io.avro

import org.scalatest._
import Matchers._

import geotrellis.raster._

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

  def roundTripWithNoDataCheck[T <% NoDataValueChecker[T]](thing: T)(implicit codec: AvroRecordCodec[T]): Unit = {
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
  trait NoDataValueChecker[T] {
    def checkNoData(json: String): Unit = {
      val noDataParsed: Option[Any] = extractNoData(json)
      doCheck(noDataParsed)
    }
    def extractNoData(json: String): Option[Any] = {
      JSON.parseFull(json) map {
        case m: Map[String,Any] => m("noDataValue")
      }
    }
    def doCheck(noData: Option[Any]): Unit = ()
  }
  implicit def shortNoDataChecker(tile: ShortArrayTile): NoDataValueChecker[ShortArrayTile] = shortNoDataChecker(tile.cellType)
  
  def shortNoDataChecker(cellType: CellType): NoDataValueChecker[ShortArrayTile] = new NoDataValueChecker[ShortArrayTile] {
    override def doCheck(nodata: Option[Any]): Unit = {
      cellType match {
        case ShortConstantNoDataCellType => nodata shouldBe Some(Map("int" -> shortNODATA.toInt))
        case ShortUserDefinedNoDataCellType(nd) => nodata shouldBe Some(Map("int" -> nd))
        case ShortCellType => nodata shouldBe Some(null)
        case _ => sys.error(s"Cell type ${cellType} was unexpected")
      }
    }
  }

  implicit def uShortNoDataChecker(tile: UShortArrayTile): NoDataValueChecker[UShortArrayTile] = uShortNoDataChecker(tile.cellType)

  def uShortNoDataChecker(cellType: CellType): NoDataValueChecker[UShortArrayTile] = new NoDataValueChecker[UShortArrayTile] {
    override def doCheck(nodata: Option[Any]): Unit = {
      cellType match {
        case UShortConstantNoDataCellType => nodata shouldBe Some(Map("int" -> ushortNODATA.toInt))
        case UShortUserDefinedNoDataCellType(nd) => nodata shouldBe Some(Map("int" -> nd))
        case UShortCellType => nodata shouldBe Some(null)
        case _ => sys.error(s"Cell type ${cellType} was unexpected")
      }
    }
  }

  implicit def intNoDataChecker(tile: IntArrayTile): NoDataValueChecker[IntArrayTile] = intNoDataChecker(tile.cellType)

  def intNoDataChecker(cellType: CellType): NoDataValueChecker[IntArrayTile] = new NoDataValueChecker[IntArrayTile] {
    override def doCheck(nodata: Option[Any]): Unit = {
      cellType match {
        case IntConstantNoDataCellType => nodata shouldBe Some(Map("int" -> NODATA))
        case IntUserDefinedNoDataCellType(nd) => nodata shouldBe Some(Map("int" -> nd))
        case IntCellType => nodata shouldBe Some(null)
        case _ => sys.error(s"Cell type ${cellType} was unexpected")
      }
    }
  }

  implicit def floatNoDataChecker(tile: FloatArrayTile): NoDataValueChecker[FloatArrayTile] = floatNoDataChecker(tile.cellType)

  def floatNoDataChecker(cellType: CellType): NoDataValueChecker[FloatArrayTile] = new NoDataValueChecker[FloatArrayTile] {
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

  implicit def doubleNoDataChecker(tile: DoubleArrayTile): NoDataValueChecker[DoubleArrayTile] = doubleNoDataChecker(tile.cellType)

  def doubleNoDataChecker(cellType: CellType): NoDataValueChecker[DoubleArrayTile] = new NoDataValueChecker[DoubleArrayTile] {
    override def doCheck(nodata: Option[Any]): Unit = {
      cellType match {
        case DoubleConstantNoDataCellType => nodata shouldBe Some(Map("boolean" -> true))
        case DoubleUserDefinedNoDataCellType(nd) => nodata shouldBe Some(Map("double" -> nd))
        case DoubleCellType => nodata shouldBe Some(Map("boolean" -> false))
        case _ => sys.error(s"Cell type ${cellType} was unexpected")
      }
    }
  }

  implicit def byteNoDataChecker(tile: ByteArrayTile): NoDataValueChecker[ByteArrayTile] = byteNoDataChecker(tile.cellType)

  def byteNoDataChecker(cellType: CellType): NoDataValueChecker[ByteArrayTile] = new NoDataValueChecker[ByteArrayTile] {
    override def doCheck(nodata: Option[Any]): Unit = {
      cellType match {
        case ByteConstantNoDataCellType => nodata shouldBe Some(Map("int" -> byteNODATA))
        case ByteUserDefinedNoDataCellType(nd) => nodata shouldBe Some(Map("int" -> nd))
        case ByteCellType => nodata shouldBe Some(null)
        case _ => sys.error(s"Cell type ${cellType} was unexpected")
      }
    }
  }

  implicit def ubyteNoDataChecker(tile: UByteArrayTile): NoDataValueChecker[UByteArrayTile] = ubyteNoDataChecker(tile.cellType)

  def ubyteNoDataChecker(cellType: CellType): NoDataValueChecker[UByteArrayTile] = new NoDataValueChecker[UByteArrayTile] {
    override def doCheck(nodata: Option[Any]): Unit = {
      cellType match {
        case UByteConstantNoDataCellType => nodata shouldBe Some(Map("int" -> ubyteNODATA))
        case UByteUserDefinedNoDataCellType(nd) => nodata shouldBe Some(Map("int" -> nd))
        case UByteCellType => nodata shouldBe Some(null)
        case _ => sys.error(s"Cell type ${cellType} was unexpected")
      }
    }
  }

  implicit def bitNoDataChecker(tile: BitArrayTile): NoDataValueChecker[BitArrayTile] = bitNoDataChecker(tile.cellType)

  def bitNoDataChecker(cellType: CellType): NoDataValueChecker[BitArrayTile] = new NoDataValueChecker[BitArrayTile] {
    override def doCheck(nodata: Option[Any]): Unit = {
      nodata shouldBe None
    }
  }

  implicit def multibandNoDataChecker(tile: MultibandTile): NoDataValueChecker[MultibandTile] =
    new NoDataValueChecker[MultibandTile] {
      override def checkNoData(json: String): Unit = {
        JSON.parseFull(json) foreach {
            case m: Map[String, Seq[Map[String, Any]]] =>
              m("bands") foreach { case bandWrapper : Map[String, Map[String, Any]] =>
                bandWrapper(bandWrapper.keys.head) match { case band: Map[String,Any] =>
                  val nodata = band.get("noDataValue")
                  tile.cellType match {
                    case ct: ShortCells =>   shortNoDataChecker(ct).doCheck(nodata)
                    case ct: UShortCells =>  uShortNoDataChecker(ct).doCheck(nodata)
                    case ct: IntCells =>     intNoDataChecker(ct).doCheck(nodata)
                    case ct: FloatCells =>   floatNoDataChecker(ct).doCheck(nodata)
                    case ct: DoubleCells =>  doubleNoDataChecker(ct).doCheck(nodata)
                    case ct: ByteCells =>    byteNoDataChecker(ct).doCheck(nodata)
                    case ct: UByteCells =>   ubyteNoDataChecker(ct).doCheck(nodata)
                    case ct: BitCells =>     bitNoDataChecker(ct).doCheck(nodata)
                    case _ => sys.error(s"Cell type ${tile.cellType} was unexpected")
                  }
                }
              }
          }
      }
    }

}
