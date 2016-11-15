package geotrellis.spark.io.hadoop.formats

import org.apache.hadoop.fs._

import org.apache.hadoop.mapreduce._
import org.apache.hadoop.mapreduce.lib.input._

class BytesFileInputFormat extends FileInputFormat[Path, Array[Byte]] {
  override def isSplitable(context: JobContext, fileName: Path) = false

  override def createRecordReader(split: InputSplit, context: TaskAttemptContext): RecordReader[Path, Array[Byte]] = {
    new BinaryFileRecordReader( bytes => split.asInstanceOf[FileSplit].getPath -> bytes)
  }
}
