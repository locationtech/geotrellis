package geotrellis.raster.render

import java.io.{FileOutputStream, File}

sealed trait ImageFormat

case class Jpg(bytes: Array[Byte]) extends ImageFormat {
  def write(path: String):Unit  = {
    val fos = new FileOutputStream(new File(path))
    try {
      fos.write(bytes)
    } finally {
      fos.close
    }
  }
}

case class Png(bytes: Array[Byte]) extends ImageFormat {
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

object Png {
  implicit def pngToArrayByte(png: Png): Array[Byte] =
    png.bytes

  implicit def arrayByteToPng(arr: Array[Byte]): Png =
    Png(arr)
}
