package geotrellis.spark.utils
import org.apache.hadoop.conf.Configuration

import org.apache.hadoop.fs.FileStatus
import org.apache.hadoop.fs.FileSystem
import org.apache.hadoop.fs.LocalFileSystem
import org.apache.hadoop.fs.Path

import java.io.BufferedReader
import java.io.Closeable
import java.io.File
import java.io.FileReader
import java.io.InputStreamReader
import java.util.Scanner

import scala.collection.mutable.ListBuffer

abstract class LineScanner extends Iterator[String] with Closeable

object HdfsUtils {
  def blockSize(conf: Configuration): Long = conf.getLong("dfs.blocksize", 64 * 1024 * 1024)
  
  def listFiles(path: Path, conf: Configuration): List[Path] = {
    val fs = path.getFileSystem(conf)
    val files = new ListBuffer[Path]
    addFiles(fs.listStatus(path), fs, conf, files)
    files.toList
  }
  def getLineScanner(path: String, conf: Configuration): Option[LineScanner] =
    getLineScanner(new Path(path), conf)

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

  private def addFiles(fileStatuses: Array[FileStatus],
                       fs: FileSystem,
                       conf: Configuration,
                       files: ListBuffer[Path]): Unit = {
    for (fst <- fileStatuses) {
      if (fst.isDir())
        addFiles(fs.listStatus(fst.getPath()), fs, conf, files)
      else
        files += fst.getPath()

    }
  }
}
