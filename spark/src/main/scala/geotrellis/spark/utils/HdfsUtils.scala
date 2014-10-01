/*
 * Copyright (c) 2014 DigitalGlobe.
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

package geotrellis.spark.utils

import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.FileStatus
import org.apache.hadoop.fs.FileSystem
import org.apache.hadoop.fs.LocalFileSystem
import org.apache.hadoop.fs.Path
import org.apache.hadoop.mapreduce.Job
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat
import org.apache.hadoop.io._

import java.io.BufferedReader
import java.io.Closeable
import java.io.File
import java.io.FileReader
import java.io.InputStreamReader
import java.util.Scanner

import scala.collection.mutable.ListBuffer
import scala.util.Random

abstract class LineScanner extends Iterator[String] with Closeable

object HdfsUtils {
  
  def putFilesInConf(filesAsCsv: String, inConf: Configuration): Configuration = {
    val job = Job.getInstance(inConf)
    FileInputFormat.setInputPaths(job, filesAsCsv)
    job.getConfiguration()
  }
  
  /* get the default block size for that path */
  def defaultBlockSize(path: Path, conf: Configuration): Long =
    path.getFileSystem(conf).getDefaultBlockSize(path)

  /* 
   * Recursively descend into a directory and and get list of file paths
   * The input path can have glob patterns
   *    e.g. /geotrellis/images/ne*.tif
   * to only return those files that match "ne*.tif" 
   */ 
  def listFiles(path: Path, conf: Configuration): List[Path] = {
    val fs = path.getFileSystem(conf)
    val files = new ListBuffer[Path]

    def addFiles(fileStatuses: Array[FileStatus]): Unit = {
      for (fst <- fileStatuses) {
        if (fst.isDirectory())
          addFiles(fs.listStatus(fst.getPath()))
        else
          files += fst.getPath()
      }
    }

    addFiles(fs.globStatus(path))
    files.toList
  }

  def getSequenceFileReader(fs: FileSystem, path: Path, conf: Configuration ): SequenceFile.Reader = {
    val (uri, dir) = (fs.getUri(), fs.getWorkingDirectory())
    new SequenceFile.Reader(conf, SequenceFile.Reader.file(path.makeQualified(uri, dir)))
  }

  /* get hadoop's temporary directory */
  def getTempDir(conf: Configuration): String = conf.get("hadoop.tmp.dir", "/tmp")

  /* 
   * Create a temporary directory called "dir" under hadoop's temporary directory.  
   * If "dir" is empty, it generates a random 40-character string as the directory name
   */ 
  def createTempDir(conf: Configuration, dir: String = ""): Path = {
    val dirPath = if (dir == "") new Path(getTempDir(conf), createRandomString(40)) else new Path(dir)
    dirPath.getFileSystem(conf).mkdirs(dirPath)
    dirPath
  }

  def createTempFile(conf: Configuration, prefix: String = ""): Path = {
    val tmpDir = new Path(getTempDir(conf))
    val tmpFile = prefix match {
      case "" => new Path(tmpDir, createRandomString(40))
      case fx => new Path(tmpDir, s"$prefix-${createRandomString(40)}")
    }
    tmpDir.getFileSystem(conf).create(tmpFile)
    tmpFile
  }


  sealed trait LocalPath { val path: Path }
  object LocalPath {
    case class Original(path: Path) extends LocalPath
    case class Temporary(path: Path) extends LocalPath
  }

  def localCopy(conf: Configuration, path: Path): LocalPath =
    path.toUri.getScheme match {
      case "file" =>
        LocalPath.Original(path)
      case _ =>
        val tmp = HdfsUtils.createTempFile(conf, "local-copy")
        val fs = tmp.getFileSystem(conf)
        fs.copyToLocalFile(path, tmp)
        LocalPath.Temporary(tmp)
    }

  def createRandomString(size: Int): String = Random.alphanumeric.take(size).mkString

  def getLineScanner(path: String, conf: Configuration): Option[LineScanner] =
    getLineScanner(new Path(path), conf)

  def readBytes(path: Path, conf: Configuration): Array[Byte] = {
    val fs = path.getFileSystem(conf)

    val len =
      fs.getFileStatus(path).getLen match {
        case l if l > Int.MaxValue.toLong =>
          sys.error(s"Cannot read path $path because it's too big..." +
            "you must tile your rasters to smaller tiles!")
        case l => l.toInt
      }

    val bytes = Array.ofDim[Byte](len)

    val stream = fs.open(path)

    try {
      stream.readFully(0, bytes)
    } finally {
      stream.close()
    }

    bytes
  }

  def getLineScanner(path: Path, conf: Configuration): Option[LineScanner] = {
    path.getFileSystem(conf) match {
      case localFS: LocalFileSystem =>
        val localFile = new File(path.toUri.getPath)
        if (!localFile.exists)
          return None
        else {
          val scanner =
            new Scanner(new BufferedReader(new FileReader(localFile)))

          val lineScanner =
            new LineScanner {
              def hasNext = scanner.hasNextLine
              def next = scanner.nextLine
              def close = scanner.close
            }

          Some(lineScanner)
        }
      case fs =>
        if (!fs.exists(path)) {
          return None
        }
        else {
          val fdis = fs.open(path)
          val scanner = new Scanner(new BufferedReader(new InputStreamReader(fdis)))

          val lineScanner =
            new LineScanner {
              def hasNext = scanner.hasNextLine
              def next = scanner.nextLine
              def close = { scanner.close; fdis.close }
            }

          Some(lineScanner)
        }
    }
  }
}
