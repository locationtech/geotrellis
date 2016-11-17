/*
 * Copyright 2016 Azavea
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
