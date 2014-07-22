package geotrellis.raster.render

import java.io.{FileOutputStream, File}

case class Png(bytes: Array[Byte]) {
  def write(path: String):Unit  = {
    val fos = new FileOutputStream(new File(path))
    try {
      fos.write(bytes)
    } finally {
      fos.close
    }
  }
}

object Png {
  implicit def pngToArrayByte(png: Png): Array[Byte] =
    png.bytes

  implicit def arrayByteToPng(arr: Array[Byte]): Png =
    Png(arr)
}
