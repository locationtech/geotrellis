/*
 * Copyright 2018 Azavea
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

import java.util.zip.ZipFile
import java.io.FileInputStream
import java.io.FileOutputStream
import scala.collection.JavaConverters._
import java.util.zip.ZipEntry
import java.io.InputStream
import java.io.OutputStream
import java.io.File

object Unzip {

  val BUFSIZE = 4096
  val buffer = new Array[Byte](BUFSIZE)
 
  def apply(source: String, targetFolder: String): Unit = {
    println(s"Unzipping: $source  -> $targetFolder")
    val zipFile = new ZipFile(source)
 
    unzipAllFile(zipFile.entries.asScala.toList, getZipEntryInputStream(zipFile)_, new File(targetFolder))
  }
 
  def getZipEntryInputStream(zipFile: ZipFile)(entry: ZipEntry) = zipFile.getInputStream(entry)
 
  def unzipAllFile(entryList: List[ZipEntry], inputGetter: (ZipEntry) => InputStream, targetFolder: File): Boolean = {    
    entryList match {
      case entry :: entries =>
 
        if (entry.isDirectory)
          new File(targetFolder, entry.getName).mkdirs
        else
          saveFile(inputGetter(entry), new FileOutputStream(new File(targetFolder, entry.getName)))
 
        unzipAllFile(entries, inputGetter, targetFolder)
      case _ =>
        true
    }
 
  }
  
  def saveFile(fis: InputStream, fos: OutputStream) = {
    writeToFile(bufferReader(fis)_, fos)
    fis.close
    fos.close
  }
 
  def bufferReader(fis: InputStream)(buffer: Array[Byte]) = (fis.read(buffer), buffer)
 
  def writeToFile(reader: (Array[Byte]) => Tuple2[Int, Array[Byte]], fos: OutputStream): Boolean = {
    val (length, data) = reader(buffer)
    if (length >= 0) {
      fos.write(data, 0, length)
      writeToFile(reader, fos)
    } else
      true
  }

  def geoTiffTestFiles(): Unit = {
    val testArchive = "raster/data/geotiff-test-files.zip"
    val testDirPath = "raster/data/geotiff-test-files"
    if(!(new File(testDirPath)).exists) {
      Unzip(testArchive, "raster/data")
    }
  }
}