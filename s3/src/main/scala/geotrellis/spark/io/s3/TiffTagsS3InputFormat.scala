package geotrellis.spark.io.s3

import geotrellis.util.StreamByteReader
import geotrellis.raster.io.geotiff.reader.TiffTagsReader
import geotrellis.raster.io.geotiff.tags.TiffTags
import geotrellis.spark.io.s3.util.S3BytesStreamer

import org.apache.hadoop.mapreduce.{InputSplit, TaskAttemptContext}
import com.amazonaws.services.s3.model._

class TiffTagsS3InputFormat extends S3InputFormat[GetObjectRequest, TiffTags] {
  def createRecordReader(split: InputSplit, context: TaskAttemptContext) =
    new TiffTagsS3RecordReader(context)
}

class TiffTagsS3RecordReader(context: TaskAttemptContext) extends S3RecordReader[GetObjectRequest, TiffTags] {
  def read(key: String, bytes: Array[Byte]) = ???

  def read(bucket: String, key: String, bytes: Array[Byte]) = {
    val tiffTags = TiffTagsReader.read(bytes)
    (new GetObjectRequest(bucket, key), tiffTags)
  }
  def read(bucket: String, key: String, reader: StreamByteReader) = {
    val tiffTags = TiffTagsReader.read(reader)
    (new GetObjectRequest(bucket, key), tiffTags)
  }
}
