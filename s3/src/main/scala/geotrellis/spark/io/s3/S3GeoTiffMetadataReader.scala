package geotrellis.spark.io.s3

import geotrellis.raster.io.geotiff.reader.TiffTagsReader
import geotrellis.raster.io.geotiff.tags.TiffTags
import geotrellis.spark.io.RasterReader
import geotrellis.spark.io.s3.util.S3RangeReader
import geotrellis.util.LazyLogging

import com.amazonaws.services.s3.model.ListObjectsRequest

case class S3GeoTiffMetadataReader(
  bucket: String,
  prefix: String,
  getS3Client: () => S3Client = () => S3Client.DEFAULT,
  delimiter: Option[String] = None
) extends LazyLogging {
  lazy val tiffTags: List[TiffTags] = {
    val s3Client = getS3Client()

    val listObjectsRequest =
      delimiter
        .fold(new ListObjectsRequest(bucket, prefix, null, null, null))(new ListObjectsRequest(bucket, prefix, null, _, null))

    s3Client
      .listKeys(listObjectsRequest)
      .map(key => TiffTagsReader.read(S3RangeReader(bucket, key, s3Client)))
      .toList
  }

  lazy val averagePixelSize: Option[Int] =
    if(tiffTags.nonEmpty) {
      Some((tiffTags.map(_.bytesPerPixel.toLong).sum / tiffTags.length).toInt)
    } else {
      logger.error(s"No tiff tags in $bucket/$prefix")
      None
    }

  def windowsCount(maxTileSize: Option[Int] = None): Int =
    tiffTags
      .flatMap { tiffTag => RasterReader.listWindows(tiffTag.cols, tiffTag.rows, maxTileSize) }
      .length

  def estimatePartitionsNumber(partitionBytes: Long, maxTileSize: Option[Int] = None): Option[Int] = {
    (maxTileSize, averagePixelSize) match {
      case (Some(tileSize), Some(pixelSize)) =>
        val numPartitions = (tileSize * pixelSize * windowsCount(maxTileSize) / partitionBytes).toInt
        logger.info(s"Estimated partitions number: $numPartitions")
        if (numPartitions > 0) Some(numPartitions)
        else None
      case _ =>
        logger.error("Can't estimate partitions number")
        None
    }
  }
}

object S3GeoTiffMetadataReader {
  def apply(
    bucket: String,
    prefix: String,
    options: S3GeoTiffRDD.Options
  ): S3GeoTiffMetadataReader = S3GeoTiffMetadataReader(bucket, prefix, options.getS3Client, options.delimiter)
}
