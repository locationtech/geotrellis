package geotrellis.data.arg

import geotrellis._
import geotrellis.data._
import geotrellis.process._

object ArgReader extends FileReader {
  def makeReadState(d:Either[String, Array[Byte]],
                    rl:RasterLayer,
                    re:RasterExtent): ReadState = rl.datatyp match {
    case "bool" => new Int1ReadState(d, rl, re)
    case "int8" => new Int8ReadState(d, rl, re)
    case "int16" => new Int16ReadState(d, rl, re)
    case "int32" => new Int32ReadState(d, rl, re)
    case "float32" => new Float32ReadState(d, rl, re)
    case "float64" => new Float64ReadState(d, rl, re)
    case t => sys.error("datatype %s is not supported" format t)
  }

  def readStateFromCache(b:Array[Byte], rl:RasterLayer, re:RasterExtent) = {
    makeReadState(Right(b), rl, re)
  }

  def readStateFromPath(p:String, rl:RasterLayer, re:RasterExtent) = {
    makeReadState(Left(p), rl, re)
  }
}
