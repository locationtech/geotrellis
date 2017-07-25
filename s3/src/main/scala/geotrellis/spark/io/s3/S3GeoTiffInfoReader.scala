package geotrellis.spark.io.s3

import geotrellis.raster.io.geotiff.reader.GeoTiffReader
import geotrellis.spark.io.RasterReader
import geotrellis.spark.io.s3.util.S3RangeReader
import geotrellis.raster.io.geotiff.reader.GeoTiffReader.GeoTiffInfo
import geotrellis.util.LazyLogging

import com.amazonaws.services.s3.model.ListObjectsRequest
import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD

import scala.collection.mutable

case class S3GeoTiffInfoReader(
  bucket: String,
  prefix: String,
  getS3Client: () => S3Client = () => S3Client.DEFAULT,
  delimiter: Option[String] = None,
  decompress: Boolean = false,
  streaming: Boolean = true
) extends LazyLogging {
  lazy val geoTiffInfo: List[(String, GeoTiffReader.GeoTiffInfo)] = {
    val s3Client = getS3Client()

    val listObjectsRequest =
      delimiter
        .fold(new ListObjectsRequest(bucket, prefix, null, null, null))(new ListObjectsRequest(bucket, prefix, null, _, null))

    s3Client
      .listKeys(listObjectsRequest)
      .toList
      .map(key => (key, GeoTiffReader.readGeoTiffInfo(S3RangeReader(bucket, key, s3Client), decompress, streaming)))
  }

  def geoTiffInfoRdd(implicit sc: SparkContext): RDD[(String, GeoTiffReader.GeoTiffInfo)] = {
    val listObjectsRequest =
      delimiter
        .fold(new ListObjectsRequest(bucket, prefix, null, null, null))(new ListObjectsRequest(bucket, prefix, null, _, null))

    sc.parallelize(getS3Client().listKeys(listObjectsRequest))
      .map(key => (key, GeoTiffReader.readGeoTiffInfo(S3RangeReader(bucket, key, getS3Client()), decompress, streaming)))
  }

  lazy val averagePixelSize: Option[Int] =
    if(geoTiffInfo.nonEmpty) {
      Some((geoTiffInfo.map(_._2.bandType.bytesPerSample.toLong).sum / geoTiffInfo.length).toInt)
    } else {
      logger.error(s"No tiff tags in $bucket/$prefix")
      None
    }

  def windowsCount(maxTileSize: Option[Int] = None): Int =
    geoTiffInfo
      .flatMap { case (_, info) =>
        RasterReader.listWindows(info.segmentLayout.totalCols, info.segmentLayout.totalRows, maxTileSize)
      }
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

  /** Returns ((key, GeoTiffInfo), List[SegmentIndicies]) */
  def segmentsByPartitionBytes(partitionBytes: Long = Long.MaxValue, maxTileSize: Option[Int] = None)
                              (implicit sc: SparkContext): RDD[((String, GeoTiffInfo), List[Int])] = {
    geoTiffInfoRdd.flatMap { case (key: String, md: GeoTiffInfo) =>
      val bufferKey = key -> md

      val allSegments = mutable.Set(md.segmentBytes.indices: _*)
      val allSegmentsInitialSize = allSegments.size

      val layout = md.segmentLayout
      val segmentBytes = md.segmentBytes

      // list of desired windows, we'll try to pack them with segments if its possible
      val windows = RasterReader.listWindows(layout.totalCols, layout.totalRows, maxTileSize)
      // a buffer with segments refs
      val buf: mutable.ListBuffer[((String, GeoTiffInfo), List[Int])] = mutable.ListBuffer()

      // walk though all desired windows
      windows.foreach { gb =>
        // buffer of segments which should fit bytes size & window size
        val windowsBuffer: mutable.ListBuffer[List[Int]] = mutable.ListBuffer() // a buffer with segments refs
        // current buffer
        val currentBuffer: mutable.ListBuffer[Int] = mutable.ListBuffer()
        var currentSize = 0
        var currentBoundsLength = 0

        // go through all segments which intersect desired bounds and was not put into any partition yet
        layout.intersectingSegments(gb).intersect(allSegments.toSeq).foreach { i =>
          val segmentSize = layout.getSegmentSize(i)
          val segmentSizeBytes = segmentBytes.getSegmentByteCount(i) * md.bandCount
          val segmentGb = layout.getGridBounds(i)

          // if segment is inside the window
          if ((gb.contains(segmentGb) && layout.isTiled) || segmentSize <= gb.size && layout.isStriped) {
            // if segment fits partition
            if (segmentSizeBytes <= partitionBytes) {
              // check if we still want to put segment into the same partition
              if (currentSize <= partitionBytes && (layout.isTiled || layout.isStriped && currentBoundsLength <= gb.size)) {
                currentSize += segmentSizeBytes
                currentBuffer += i
                currentBoundsLength += segmentSize
              } else { // or put it into a separate partition
                windowsBuffer += currentBuffer.toList
                currentBuffer.clear()
                currentSize = segmentSizeBytes
                currentBoundsLength = segmentSize
                currentBuffer += i
              }
            } else {
              // case when segment size is bigger than a desired partition size
              // it is better to use a different strategy for these purposes
              logger.warn("Segment size is bigger than a desired partition size, " +
                "though it fits the window size. You can consider a different partitioning strategy.")
              windowsBuffer += List(i)
            }
            allSegments -= i
          }
        }

        // if we have smth left in the current buffer
        if(currentBuffer.nonEmpty) windowsBuffer += currentBuffer.toList

        windowsBuffer.foreach { indices => buf += (bufferKey -> indices) }
      }

      // there can be left some windows
      if (allSegments.nonEmpty) {
        logger.warn(s"Some segments don't fit windows (${allSegments.size} of $allSegmentsInitialSize).")

        val windowsBuffer: mutable.ListBuffer[List[Int]] = mutable.ListBuffer() // a buffer with segments refs
        val currentBuffer: mutable.ListBuffer[Int] = mutable.ListBuffer()
        val gbSize = windows.head.size
        var currentSize = 0
        var currentBoundsLength = 0

        allSegments.foreach { i =>
          val segmentSize = layout.getSegmentSize(i)
          val segmentSizeBytes = segmentBytes.getSegmentByteCount(i) * md.bandCount

          if (currentSize <= partitionBytes) {
            if (currentSize <= partitionBytes && (layout.isTiled || layout.isStriped && currentBoundsLength <= gbSize)) {
              currentSize += segmentSizeBytes
              currentBuffer += i
              currentBoundsLength += segmentSize
            } else {
              windowsBuffer += currentBuffer.toList
              currentBuffer.clear()
              currentSize = segmentSizeBytes
              currentBoundsLength = segmentSize
              currentBuffer += i
            }
          } else {
            // case when segment size is bigger than a desired partition size
            // it is better to use a different strategy for these purposes
            logger.warn("Segment size is bigger than a desired partition size, " +
              "and it doesn't fit window a desired window size. You can consider a different partitioning strategy.")
            windowsBuffer += List(i)
          }
        }

        // if we have smth left in the current buffer
        if(currentBuffer.nonEmpty) windowsBuffer += currentBuffer.toList

        windowsBuffer.foreach { indices => buf += (bufferKey -> indices) }
      }

      buf
    }
  }
}

object S3GeoTiffInfoReader {
  def apply(
    bucket: String,
    prefix: String,
    options: S3GeoTiffRDD.Options
  ): S3GeoTiffInfoReader = S3GeoTiffInfoReader(bucket, prefix, options.getS3Client, options.delimiter)
}
