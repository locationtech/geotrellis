package geotrellis.spark.io.s3

import geotrellis.util.ByteReader
import geotrellis.raster.io.geotiff.reader.TiffTagsReader
import geotrellis.raster.io.geotiff.tags.TiffTags

import org.apache.hadoop.mapreduce.{InputSplit, TaskAttemptContext}
import com.amazonaws.services.s3.model._

/**
 * This class extends [[S3InputFormat]] and is used to create RDDs of TiffTags.
 * from files on S3.
 */
class TiffTagsS3InputFormat extends S3InputFormat[GetObjectRequest, TiffTags] {

  /**
   * Creates a RecordReader that can be used to read [[TiffTags]] from S3.
   *
   * @param split: The [[InputSplit]] that contains the data to make the RDD.
   * @param context: The [[TaskAttemptContext]] of the InputSplit.
   *
   * @return A [[RecordReader]] that can read [[TiffTags]] of GeoTiffs from S3.
   */
  def createRecordReader(split: InputSplit, context: TaskAttemptContext) =
    new StreamingS3RecordReader[GetObjectRequest, TiffTags](getS3Client(context)) {

      /**
       * Creates a RDD that has a key value pair of GetObjectRequest and TiffTags, respectively.
       *
       * @param key: A `String` that is the key of the file to be read.
       * @param reader: A [[ByteReader]] that will be used to read the actual TiffTags from the file.
       *
       * @return An RDD that has a key of type [[GetObjectRequest]] and a key of [[TiffTags]].
       */
      def read(key: String, reader: ByteReader) = {
        val tiffTags = TiffTagsReader.read(reader)
        (new GetObjectRequest(bucket, key), tiffTags)
      }
    }
}
