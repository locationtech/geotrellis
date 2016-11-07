package geotrellis.spark.io.s3

import geotrellis.util.ByteReader
import geotrellis.raster.io.geotiff.reader.TiffTagsReader
import geotrellis.raster.io.geotiff.tags.TiffTags

import org.apache.hadoop.mapreduce.{InputSplit, TaskAttemptContext}
import com.amazonaws.services.s3.model._

class TiffTagsS3InputFormat extends S3InputFormat[GetObjectRequest, TiffTags] {
  def createRecordReader(split: InputSplit, context: TaskAttemptContext) =
    new StreamingS3RecordReader[GetObjectRequest, TiffTags](getS3Client(context)) {
      def read(key: String, reader: ByteReader) = {
        val tiffTags = TiffTagsReader.read(reader)
        (new GetObjectRequest(bucket, key), tiffTags)
      }
    }

}
