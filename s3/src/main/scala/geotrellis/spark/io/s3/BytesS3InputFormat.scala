package geotrellis.spark.io.s3

import org.apache.hadoop.mapreduce.{InputSplit, TaskAttemptContext}

class BytesS3InputFormat extends S3InputFormat[String, Array[Byte]] {
  def createRecordReader(split: InputSplit, context: TaskAttemptContext) = {
    val s3Client = getS3Client(context)
    new S3RecordReader[String, Array[Byte]](s3Client) {
      def read(key: String, bytes: Array[Byte]) = (key, bytes)
    }
  }
}
