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

import geotrellis.raster.render._
import geotrellis.raster._

import java.io.{File, ByteArrayOutputStream}
import java.nio.file.Files
import java.io.OutputStream
import javax.imageio._
import javax.imageio.plugins.jpeg._
import javax.imageio.stream._
import java.awt.image.BufferedImage
import java.util.Locale

import scala.math.abs

import spire.syntax.cfor._

case class JpgEncoder(settings: Settings = Settings.DEFAULT) {

  def writeParams: ImageWriteParam = {
    val writeParams = new JPEGImageWriteParam(Locale.getDefault())
    writeParams.setCompressionMode(ImageWriteParam.MODE_EXPLICIT)
    writeParams.setCompressionQuality(settings.compressionQuality.toFloat)
    writeParams.setOptimizeHuffmanTables(settings.optimize)
    writeParams
  }

  def writeOutputStream(os: ImageOutputStream, raster: Tile) {
    val img: BufferedImage = raster.toBufferedImage

    // Write to provided output stream
    val writer: ImageWriter = ImageIO.getImageWritersByFormatName("jpg").next()
    writer.setOutput(os)
    writer.write(null, new IIOImage(img, null, null), this.writeParams)
    writer.dispose()
  }

  def writeByteArray(raster: Tile): Array[Byte] = {
    val baos = new ByteArrayOutputStream
    val cacheDir = Files.createTempDirectory("foobar").toFile()
    cacheDir.deleteOnExit()
    val fcios = new FileCacheImageOutputStream(baos, cacheDir)

    writeOutputStream(fcios, raster)
    fcios.flush()
    baos.flush()

    val bytes = baos.toByteArray
    fcios.close()
    baos.close()
    cacheDir.delete()
    bytes
  }

  def writePath(path: String, raster: Tile): Unit = {
    val fios = new FileImageOutputStream(new File(path))
    writeOutputStream(fios, raster)
    fios.flush()
    fios.close()
  }
}

