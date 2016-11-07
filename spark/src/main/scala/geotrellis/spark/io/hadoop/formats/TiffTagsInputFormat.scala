package geotrellis.spark.io.hadoop.formats

import geotrellis.raster.io.geotiff.tags.TiffTags
import geotrellis.raster.io.geotiff.reader.TiffTagsReader
import geotrellis.spark.io.hadoop._
import geotrellis.util.StreamingByteReader

import org.apache.hadoop.fs._
import org.apache.hadoop.mapreduce._
import org.apache.hadoop.mapreduce.lib.input._

class TiffTagsInputFormat extends FileInputFormat[Path, TiffTags] {
  override def isSplitable(context: JobContext, fileName: Path) = false

  override def createRecordReader(split: InputSplit, context: TaskAttemptContext) =
    new RecordReader[Path, TiffTags] {
      private var tup: (Path, TiffTags) = null
      private var hasNext: Boolean = true

      def initialize(split: InputSplit, context: TaskAttemptContext) = {
        val path = split.asInstanceOf[FileSplit].getPath()
        val conf = context.getConfiguration()
        val byteReader = StreamingByteReader(HdfsRangeReader(path, conf))

        tup = (path, TiffTagsReader.read(byteReader))
      }

      def close = {}
      def getCurrentKey = tup._1
      def getCurrentValue = { hasNext = false ; tup._2 }
      def getProgress = 1
      def nextKeyValue = hasNext
    }
}
