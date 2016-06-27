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

package geotrellis.util

import java.io._
import java.nio.ByteBuffer
import java.nio.channels.FileChannel.MapMode._
import java.nio.charset.StandardCharsets
import java.nio.file._


object Filesystem {
  /**
    * Read the contents of a file into an array.
    *
    * @param   path The path to the file to be read
    * @param   bs   The block size; The file will be read in chunks of this size
    * @return       An array of bytes containing the file contents
    */
  def slurp(path: String, bs: Int = (1<<18)): Array[Byte] = {
    val buffer = toMappedByteBuffer(path)

    // read 256KiB (2^18 bytes) at a time out of the buffer into our array
    var i = 0
    val data = Array.ofDim[Byte](buffer.capacity)
    while(buffer.hasRemaining()) {
      val n = math.min(buffer.remaining(), bs)
      buffer.get(data, i, n)
      i += n
    }

    data
  }

  /**
    * Make a contiguous chunk of a file available in the given array.
    *
    * @param path       The path to the file which is to be (partially) mapped into memory
    * @param data       An array to contain the portion of the file in question
    * @param startIndex The offset into the file where the contiguous region begins
    * @param size       The size of the contiguous region
    */
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
    *
    * @param   path The path whose extension is to be removed (if it has one)
    * @return       The path minus the extension
    */
  def basename(path: String) =
    path.lastIndexOf(".") match {
      case -1 => path
      case n => path.substring(0, n)
    }

  /**
    * Split the given path into a tuple of base and extension.
    *
    * @param   path The path which is to be split
    * @return       A tuple of the basename and the extension of the given path
    */
  def split(path: String) =
    path.lastIndexOf(".") match {
      case -1 => (path, "")
      case n => (path.substring(0, n), path.substring(n + 1, path.length))
    }

  /**
    * Join the given parts into a single string, with the parts
    * separated by the system's file separator.
    *
    * @param   parts A list of parts which are to be concatenated
    * @return        The parts concatenated, separated by the file separator
    */
  def join(parts: String*) = parts.mkString(File.separator)

  /**
    * Return the contents of a file, interpreted as text, as a string.
    *
    * @param   path The path to the file to be read
    * @return       A string containing the file's contents
    */
  def readText(path: String): String = {
    val src = scala.io.Source.fromFile(path, "UTF-8")
    try {
      src.mkString
    } finally {
      src.close()
    }
  }

  /**
    * Return the contents of a file, interpreted as text, as a string.
    *
    * @param file The the file to be read
    * @return     A string containing the file's contents
    */
  def readText(file: File): String =
    readText(file.getAbsolutePath)

  /**
    * Write the given array of bytes to the file pointed to by the
    * given path.  This is a truncating write.
    *
    * @param path  The path to the file where the data are to be written
    * @param bytes An array of bytes containing the data to be written
    */
  def writeBytes(path: String, bytes: Array[Byte]): Unit = {
    val bos = new BufferedOutputStream(new FileOutputStream(path))
    bos.write(bytes)
    bos.close
  }

  /**
    * Write the given text into the file pointed to by the given path.
    * The is a truncating write.
    *
    * @param path The path to the file where the text will be written
    * @param text A string containing the text to be written
    */
  def writeText(path: String, text: String): Unit =
    Files.write(Paths.get(path), text.getBytes(StandardCharsets.UTF_8))

  /**
    * Write the given text into a file.  This is a truncating write.
    *
    * @param path The path to the file where the text will be written
    * @param text A string containing the text to be written
    */
  def writeText(file: File, text: String): Unit =
    writeText(file.getAbsolutePath, text)

  /**
    * Move (rename) the given file, obliterating any existing file
    * that already has the target name.
    *
    * @param source The path to the file before moving
    * @param target The path to the file after moving
    */
  def move(source: String, target: String): Unit =
    Files.move(Paths.get(source), Paths.get(target), StandardCopyOption.REPLACE_EXISTING)

  /**
    * Move (rename) the given file, obliterating any existing file
    * that already has the target name.
    *
    * @param source A [[java.nio.file.File]] representing the desired source
    * @param target A [[java.nio.file.File]] representing the desired destination
    */
  def move(source: File, target: File): Unit =
    move(source.getAbsolutePath, target.getAbsolutePath)

  /**
    * Copy the given file, obliterating any existing file that already
    * has the target name.
    *
    * @param source The path to the file to be copied
    * @param target The path to the place where the file is to be copied
    */
  def copy(source: String, target: String): Unit =
    Files.copy(Paths.get(source), Paths.get(target), StandardCopyOption.REPLACE_EXISTING)

  /**
    * Copy the given file, obliterating any existing file that already
    * has the target name.
    *
    * @param source The path to the file to be copied
    * @param target The path to the place where the file is to be copied
    */
  def copy(source: File, target: File): Unit =
    copy(source.getAbsolutePath, target.getAbsolutePath)

  /**
    * Ensure the existence of a given directory.
    *
    * @param path The path to the directory
    */
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
