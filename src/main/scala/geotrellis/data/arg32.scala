//package geotrellis.data
//
//import java.io.{BufferedOutputStream, DataOutputStream, FileOutputStream}
//import java.nio.ByteBuffer
//import java.nio.channels.FileChannel.MapMode._
//
//import geotrellis._
//import geotrellis.util._
//import geotrellis.process._
//
//abstract class ArgIntNReadState(data:Either[String, Array[Byte]],
//                             val layer:RasterLayer,
//                             val target:RasterExtent,
//                             typ:RasterType) extends IntReadState {
//  def getType = typ
//
//  final val width:Int = typ.bits / 8
//
//  protected[this] var src:ByteBuffer = null
//
//  // NoData value is the minimum value storable w/ this bitwidth (-1 for sign)
//  def getNoDataValue:Int = -(1 << (width * 8 - 1))
//
//  def initSource(pos:Int, size:Int) {
//    src = data match {
//      case Left(path) => Filesystem.slurpToBuffer(path, pos * width, size * width)
//      case Right(bytes) => ByteBuffer.wrap(bytes, pos * width, size * width)
//    }
//  }
//}
//
//object ArgReader extends FileReader {
//  def makeReadState(d:Either[String, Array[Byte]],
//                    rl:RasterLayer,
//                    re:RasterExtent): ReadState = rl.datatyp match {
//    case "int8" => new Int8ReadState(d, rl, re)
//    case "int16" => new Int16ReadState(d, rl, re)
//    case "int32" => new Int32ReadState(d, rl, re)
//    case "float32" => new Float32ReadState(d, rl, re)
//    case "float64" => new Float64ReadState(d, rl, re)
//    case t => sys.error("datatype %s is not supported" format t)
//  }
//
//  def readStateFromCache(b:Array[Byte], rl:RasterLayer, re:RasterExtent) = {
//    makeReadState(Right(b), rl, re)
//  }
//
//  def readStateFromPath(p:String, rl:RasterLayer, re:RasterExtent) = {
//    makeReadState(Left(p), rl, re)
//  }
//}
//
//trait CellWriter {
//  protected[this] def writeCell(data:RasterData, col:Int, row:Int, cols:Int, dos:DataOutputStream)
//  def writeCells(raster:Raster, dos:DataOutputStream) {
//    val data = raster.data
//    val cols = raster.rasterExtent.cols
//    val rows = raster.rasterExtent.rows
//
//    var row = 0
//    while (row < rows) {
//      var col = 0
//      while (col < cols) {
//        writeCell(data, col, row, cols, dos)
//        col += 1
//      }
//      row += 1
//    }
//  }
//}
//
//object BitCellWriter extends CellWriter {
//  override def writeCells(raster:Raster, dos:DataOutputStream) {
//    val data = raster.data
//    val cols = raster.rasterExtent.cols
//    val rows = raster.rasterExtent.rows
//
//    var i = 0
//    var z = 0
//    var row = 0
//    while (row < rows) {
//      var col = 0
//      while (col < cols) {
//        z = (z << 1) | data.get(col, row, cols)
//        i += 1
//        col += 1
//        if (i == 8) {
//          i = 0
//          z = 0
//          dos.writeByte(z)
//        }
//      }
//      row += 1
//    }
//  }
//  def writeCell(data:RasterData, col:Int, row:Int, cols:Int, dos:DataOutputStream) = ()
//}
//
//object ByteCellWriter extends CellWriter {
//  @inline final def writeCell(data:RasterData, col:Int, row:Int, cols:Int, dos:DataOutputStream) {
//    dos.writeByte(data.get(col, row, cols))
//  }
//}
//
//object ShortCellWriter extends CellWriter {
//  @inline final def writeCell(data:RasterData, col:Int, row:Int, cols:Int, dos:DataOutputStream) {
//    dos.writeShort(data.get(col, row, cols))
//  }
//}
//
//object IntCellWriter extends CellWriter {
//  @inline final def writeCell(data:RasterData, col:Int, row:Int, cols:Int, dos:DataOutputStream) {
//    dos.writeInt(data.get(col, row, cols))
//  }
//}
//
//object FloatCellWriter extends CellWriter {
//  @inline final def writeCell(data:RasterData, col:Int, row:Int, cols:Int, dos:DataOutputStream) {
//    dos.writeFloat(data.getDouble(col, row, cols).toFloat)
//  }
//}
//
//object DoubleCellWriter extends CellWriter {
//  @inline final def writeCell(data:RasterData, col:Int, row:Int, cols:Int, dos:DataOutputStream) {
//    dos.writeDouble(data.getDouble(col, row, cols))
//  }
//}
//
//
//case class ArgWriter(typ:RasterType) extends Writer {
//  def width = typ.bits / 8
//  def rasterType = "arg"
//  def dataType = typ.name
//
//  def write(path:String, raster:Raster, name:String) {
//    val base = path.substring(0, path.lastIndexOf("."))
//    writeMetadataJSON(base + ".json", name, raster.rasterExtent)
//    writeData(base + ".arg", raster)
//  }
//
//  private def writeBytes(data:RasterData, cols:Int, rows:Int, dos:DataOutputStream) {
//    var row = 0
//    while (row < rows) {
//      var col = 0
//      while (col < cols) {
//        dos.writeByte(data.get(col, row, cols))
//        col += 1
//      }
//      row += 1
//    }
//  }
//
//  private def writeData(path:String, raster:Raster) {
//    val dos = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(path)))
//    val cw = typ match {
//      case TypeByte => ByteCellWriter
//      case TypeShort => ShortCellWriter
//      case TypeInt => IntCellWriter
//      case TypeFloat => FloatCellWriter
//      case TypeDouble => DoubleCellWriter
//      case t => sys.error("raster type %s is not supported yet" format t)
//    }
//    cw.writeCells(raster, dos)
//    dos.close()
//  }
//}
//
//class Int32ReadState(data:Either[String, Array[Byte]],
//                     layer:RasterLayer,
//                     target:RasterExtent)  extends ArgIntNReadState(data, layer, target, TypeInt) {
//  @inline final def assignFromSource(sourceIndex:Int, dest:StrictRasterData, destIndex:Int) {
//    dest(destIndex) = src.getInt(sourceIndex * width)
//  }
//}
//
//class Int16ReadState(data:Either[String, Array[Byte]],
//                     layer:RasterLayer,
//                     target:RasterExtent) extends ArgIntNReadState(data, layer, target, TypeShort) {
//  @inline final def assignFromSource(sourceIndex:Int, dest:StrictRasterData, destIndex:Int) {
//    dest(destIndex) = src.get(sourceIndex * width)
//  }
//}
//
//class Int8ReadState(data:Either[String, Array[Byte]],
//                    layer:RasterLayer,
//                    target:RasterExtent) extends ArgIntNReadState(data, layer, target, TypeByte) {
//  @inline final def assignFromSource(sourceIndex:Int, dest:StrictRasterData, destIndex:Int) {
//    dest(destIndex) = src.get(sourceIndex * width)
//  }
//}
//
//abstract class ArgFloatNReadState(data:Either[String, Array[Byte]],
//                               val layer:RasterLayer,
//                               val target:RasterExtent,
//                               typ:RasterType) extends ReadState {
//  def getType = typ
//
//  final val width:Int = typ.bits / 8
//
//  protected[this] var src:ByteBuffer = null
//
//  def initSource(pos:Int, size:Int) {
//    src = data match {
//      case Left(path) => Filesystem.slurpToBuffer(path, pos * width, size * width)
//      case Right(bytes) => ByteBuffer.wrap(bytes, pos * width, size * width)
//    }
//  }
//}
//
//class Float64ReadState(data:Either[String, Array[Byte]],
//                       layer:RasterLayer,
//                       target:RasterExtent)  extends ArgFloatNReadState(data, layer, target, TypeDouble) {
//  @inline final def assignFromSource(sourceIndex:Int, dest:StrictRasterData, destIndex:Int) {
//    dest.updateDouble(destIndex, src.getDouble(sourceIndex * width))
//  }
//}
//
//class Float32ReadState(data:Either[String, Array[Byte]],
//                       layer:RasterLayer,
//                       target:RasterExtent)  extends ArgFloatNReadState(data, layer, target, TypeFloat) {
//  @inline final def assignFromSource(sourceIndex:Int, dest:StrictRasterData, destIndex:Int) {
//    dest.updateDouble(destIndex, src.getFloat(sourceIndex * width))
//  }
//}
