package geotrellis.raster.render

import java.io.{FileOutputStream, File}

case class Jpg(bytes: Array[Byte]) {
  def write(path: String):Unit  = {
    val fos = new FileOutputStream(new File(path))
    try {
      fos.write(bytes)
    } finally {
      fos.close
    }
  }
}

object Jpg {
  implicit def jpgToArrayByte(jpg: Jpg): Array[Byte] =
    jpg.bytes

  implicit def arrayByteToJpg(arr: Array[Byte]): Jpg =
    Jpg(arr)
}
