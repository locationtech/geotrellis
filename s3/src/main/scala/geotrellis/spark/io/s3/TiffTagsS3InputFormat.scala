package geotrellis.spark.io.s3

import geotrellis.util.ByteReader
import geotrellis.raster.io.geotiff.reader.TiffTagsReader
import geotrellis.raster.io.geotiff.tags.TiffTags

import org.apache.hadoop.mapreduce.{InputSplit, TaskAttemptContext}
import com.amazonaws.services.s3.model._

/** Reads the tiff tags of GeoTiffs on S3, avoiding full file read. */
class TiffTagsS3InputFormat extends S3InputFormat[GetObjectRequest, TiffTags] {

  /**
   * Creates a RecordReader that can be used to read [[TiffTags]] from an S3 object.
   *
   * @param split The [[InputSplit]] that defines what records will be in this partition.
   * @param context The [[TaskAttemptContext]] of the InputSplit.
   *
   * @return A [[StreamingS3RecordReader]] that can read [[TiffTags]] of GeoTiffs from S3.
   */
  def createRecordReader(split: InputSplit, context: TaskAttemptContext) =
    new StreamingS3RecordReader[GetObjectRequest, TiffTags](getS3Client(context)) {

      /**
       * Read the header of the GeoTiff and return its tiff tags along with the corresponding [[GetObjectRequest]].
       *
       * @param key  The S3 key of the file to be read.
       * @param reader A [[ByteReader]] that must be used to read the actual TiffTags from the file.
       */
      def read(key: String, reader: ByteReader): (GetObjectRequest, TiffTags) = {
        val tiffTags = TiffTagsReader.read(reader)
        (new GetObjectRequest(bucket, key), tiffTags)
      }
    }
}
