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

package geotrellis.raster.io

import java.nio.file._
import java.nio.charset.StandardCharsets
import java.io._
import java.nio.ByteBuffer
import java.nio.channels.FileChannel.MapMode._

object Filesystem {
  def slurp(path: String, bs: Int = 262144): Array[Byte] = {
    val f = new File(path)
    val fis = new FileInputStream(f)
    val size = f.length.toInt
    val channel = fis.getChannel
    val buffer = channel.map(READ_ONLY, 0, size)
    channel.close()
    fis.close()

    // read 256K at a time out of the buffer into our array
    var i = 0
    val data = Array.ofDim[Byte](size)
    while(buffer.hasRemaining()) {
      val n = math.min(buffer.remaining(), bs)
      buffer.get(data, i, n)
      i += n
    }

    data
  }

  def mapToByteArray(path: String, data: Array[Byte], startIndex: Int, size: Int): Unit = {
    val f = new File(path)
    val fis = new FileInputStream(f)
    val buffer =
      try {
        val channel = fis.getChannel
        channel.map(READ_ONLY, startIndex, size)
      } finally {
        fis.close
      }
    buffer.get(data, startIndex, size)
  }

  /**
   * Return the path string with the final extension removed.
   */
  def basename(p: String) = p.lastIndexOf(".") match {
    case -1 => p
    case n => p.substring(0, n)
  }

  def split(p: String) = p.lastIndexOf(".") match {
    case -1 => (p, "")
    case n => (p.substring(0, n), p.substring(n + 1, p.length))
  }

  def join(parts: String*) = parts.mkString(File.separator)

  def readText(path: String): String = {
    val src = scala.io.Source.fromFile(path, "UTF-8")
    try {
      src.mkString
    } finally {
      src.close()
    }
  }

  def readText(file: File): String =
    readText(file.getAbsolutePath)

  def writeBytes(path: String, bytes: Array[Byte]): Unit = {
    val bos = new BufferedOutputStream(new FileOutputStream(path))
    bos.write(bytes)
    bos.close
  }

  def writeText(path: String, text: String): Unit =
    Files.write(Paths.get(path), text.getBytes(StandardCharsets.UTF_8))

  def writeText(file: File, text: String): Unit =
    writeText(file.getAbsolutePath, text)

  def move(source: String, target: String): Unit =
    Files.move(Paths.get(source), Paths.get(target), StandardCopyOption.REPLACE_EXISTING)

  def move(source: File, target: File): Unit =
    move(source.getAbsolutePath, target.getAbsolutePath)

  def copy(source: String, target: String): Unit =
    Files.copy(Paths.get(source), Paths.get(target), StandardCopyOption.REPLACE_EXISTING)

  def copy(source: File, target: File): Unit =
    copy(source.getAbsolutePath, target.getAbsolutePath)

  def ensureDirectory(path: String): String = {
    val f = new File(path)
    if(f.exists) {
      require(f.isDirectory, s"$f exists and is not a directory")
    } else {
      f.mkdirs()
    }
    path
  }
}
