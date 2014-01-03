package geotrellis.spark.utils

import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs._

import java.io._
import java.util.Scanner

abstract class LineScanner extends Iterator[String] with Closeable

object HdfsUtils {
  def getLineScanner(path:String, conf:Configuration): Option[LineScanner] =
    getLineScanner(new Path(path), conf)

  def getLineScanner(path:Path, conf:Configuration): Option[LineScanner] = {
    path.getFileSystem(conf) match {
      case localFS: LocalFileSystem =>
        val localSplitFile = new File(path.toUri.getPath)
        if(!localSplitFile.exists)
          return None
        else {
          val scanner =
            new Scanner(new BufferedReader(new FileReader(localSplitFile)))

          val lineScanner =
            new LineScanner {
              def hasNext = scanner.hasNextLine
              def next = scanner.nextLine
              def close = scanner.close
            }

          Some(lineScanner)
        }
      case fs =>
        if(!fs.exists(path)) {
          return None
        } else {
          val fdis = fs.open(path)
          val scanner = new Scanner(new BufferedReader(new InputStreamReader(fdis)))

          val lineScanner =
            new LineScanner {
              def hasNext = scanner.hasNextLine
              def next = scanner.nextLine
              def close = { scanner.close ; fdis.close }
            }

          Some(lineScanner)
        }
    }
  }
}
