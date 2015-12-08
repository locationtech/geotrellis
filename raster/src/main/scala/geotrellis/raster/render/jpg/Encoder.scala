/*
 * Copyright (c) 2014 Azavea.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package geotrellis.raster.render.jpg

import geotrellis.raster._

import java.io.{File, ByteArrayOutputStream, FileOutputStream}
import java.io.OutputStream
import javax.imageio.ImageIO
import javax.imageio.stream.FileImageOutputStream
import java.awt.image.BufferedImage

import scala.math.abs

import spire.syntax.cfor._

object JpgEncoder {

  def writeOutputStream(os: OutputStream, raster: Tile) {
    val bi = new BufferedImage(raster.cols, raster.rows, BufferedImage.TYPE_INT_RGB)
    cfor(0)(_ < raster.cols, _ + 1) { x =>
      cfor(0)(_ < raster.rows, _ + 1) { y =>
        bi.setRGB(x, y, raster.get(x, y))
      }
    }
    ImageIO.write(bi, "jpg", os)
  }

  def writeByteArray(raster: Tile): Array[Byte] = {
    val baos = new ByteArrayOutputStream()
    writeOutputStream(baos, raster)
    val arr = baos.toByteArray
    baos.close()
    arr
  }

  def writePath(path: String, raster: Tile): Unit = {
    val fos = new FileOutputStream(new File(path))
    writeOutputStream(fos, raster)
    fos.close()
  }
}

