package geotrellis.spark.io.hadoop.formats

import org.apache.hadoop.fs._
import org.apache.hadoop.io._
import org.apache.hadoop.mapreduce._
import org.apache.hadoop.mapreduce.lib.output._

object SplitsMapFileOutputFormat {
  def SPLITS_FILE = "splits"
}

class SplitsMapFileOutputFormat[K <: WritableComparable[K]]() extends MapFileOutputFormat {
  final val utf8 = "UTF-8"

  override
  def  getRecordWriter(context: TaskAttemptContext): RecordWriter[WritableComparable[_], Writable] = {
    val conf = context.getConfiguration()
    val dir = getDefaultWorkFile(context, "")
    val fs = dir.getFileSystem(conf)
    val splitsFile = new Path(dir, SplitsMapFileOutputFormat.SPLITS_FILE)
    val splitsFileOut = fs.create(splitsFile, false)

    val mapFileWriter = super.getRecordWriter(context)

    new RecordWriter[WritableComparable[_], Writable] {
      var minKey: Option[K] = None
      var maxKey: Option[K] = None

      def write(k: WritableComparable[_], value: Writable): Unit = {
        val key = k.asInstanceOf[K]
        mapFileWriter.write(key, value)
        minKey match {
          case None => minKey = Some(key)
          case Some(mk) if mk.compareTo(key) == -1 => minKey = Some(key)
          case _ =>
        }

        maxKey match {
          case None => maxKey = Some(key)
          case Some(mk) if mk.compareTo(key) == 1 => maxKey = Some(key)
          case _ =>
        }
      }

      def close(context: TaskAttemptContext): Unit = {
        mapFileWriter.close(context)
        if(minKey != null && maxKey != null) {
          splitsFileOut.write(minKey.toString().getBytes(utf8))
          splitsFileOut.write(maxKey.toString().getBytes(utf8))
        }
        splitsFileOut.close()
      }
    }
  }
}

