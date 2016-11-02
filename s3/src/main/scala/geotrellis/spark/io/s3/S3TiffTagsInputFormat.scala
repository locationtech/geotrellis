package geotrellis.spark.io.s3

import geotrellis.util.StreamByteReader
import geotrellis.raster.io.geotiff.reader.TiffTagsReader
import geotrellis.raster.io.geotiff.tags.TiffTags
import geotrellis.spark.io.s3.util.S3BytesStreamer

import org.apache.hadoop.mapreduce.{InputSplit, TaskAttemptContext}
import com.amazonaws.services.s3.model._

class TiffTagsS3InputFormat extends S3InputFormat[String, TiffTags] {
  def createRecordReader(split: InputSplit, context: TaskAttemptContext) =
    new TiffTagsS3RecordReader(context)
}

class TiffTagsS3RecordReader(context: TaskAttemptContext) extends S3RecordReader[String, TiffTags] {
  def read(key: String, bytes: Array[Byte]) = {
    val tiffTags = TiffTagsReader.read(bytes)
    (key, tiffTags)
  }
  def read(key: String, reader: StreamByteReader) = {
    val tiffTags = TiffTagsReader.read(reader)
    (key, tiffTags)
  }
}
