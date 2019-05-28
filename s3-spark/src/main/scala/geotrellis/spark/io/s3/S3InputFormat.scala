/*
 * Copyright 2016 Azavea
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package geotrellis.spark.store.s3

import geotrellis.store.s3.S3ClientProducer
import geotrellis.spark.store.hadoop._

import com.typesafe.scalalogging.LazyLogging

import software.amazon.awssdk.regions._
import software.amazon.awssdk.services.s3.model.{ListObjectsV2Request, S3Object}
import software.amazon.awssdk.services.s3.S3Client

import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.mapreduce.{InputFormat, InputSplit, Job, JobContext}

import scala.util.matching.Regex
import scala.collection.JavaConverters._

/** Reads keys from s3n URL using AWS Java SDK.
  * The number of keys per InputSplits are controlled by S3 pagination.
  * If AWS credentials are not part of the URL they will be discovered using [DefaultAWSCredentialsProviderChain]:
  *   - EnvironmentVariableCredentialsProvider
  *   - SystemPropertiesCredentialsProvider
  *   - ProfileCredentialsProvider
  *   - InstanceProfileCredentialsProvider
  */
abstract class S3InputFormat[K, V] extends InputFormat[K,V] with LazyLogging {
  import S3InputFormat._

  def getS3Client(context: JobContext): S3Client =
    S3InputFormat.getS3Client(context)

  override def getSplits(context: JobContext) = {

    val conf = context.getConfiguration
    val anon = conf.get(ANONYMOUS)
    val id = conf.get(AWS_ID)
    val key = conf.get(AWS_KEY)
    val bucket = conf.get(BUCKET)
    val prefix = conf.get(PREFIX)
    val crs = conf.get(CRS_VALUE)

    val extensions: Array[String] = {
      val extensionsConf = conf.get(EXTENSIONS)
      if (extensionsConf == null)
        Array()
      else
        extensionsConf.split(",").map(_.trim)
    }

    val chunkSize = {
      val chunkSizeConf = conf.get(CHUNK_SIZE)
      if (chunkSizeConf == null)
        S3InputFormat.DEFAULT_CHUNK_SIZE
      else
        chunkSizeConf
    }

    val delimiter = S3InputFormat.getDelimiter(conf)

    val partitionCountConf = conf.get(PARTITION_COUNT)
    val partitionSizeConf = conf.get(PARTITION_BYTES)
    require(null == partitionCountConf || null == partitionSizeConf,
      "Either PARTITION_COUNT or PARTITION_SIZE option may be set")

    val s3client: S3Client = getS3Client(context)

    logger.info(s"Listing Splits: bucket=$bucket prefix=$prefix")

    val request = delimiter match {
      case Some(d) =>
        ListObjectsV2Request.builder()
          .bucket(bucket)
          .prefix(prefix)
          .delimiter(d)
          .build()
      case None =>
        ListObjectsV2Request.builder()
          .bucket(bucket)
          .prefix(prefix)
          .build()
    }

    def makeNewSplit =  {
      val split = new S3InputSplit
      split.bucket = bucket
      split
    }

    var splits: Vector[S3InputSplit] = Vector(makeNewSplit)

    val s3ObjectIsTiff: S3Object => Boolean = { s3obj =>
      val key = s3obj.key

      val isDir =
        key.endsWith("/")
      val isTiff =
        if (extensions.isEmpty)
          true
        else {
          extensions.map(key.endsWith).reduce(_ || _)
        }

      !isDir && isTiff
    }

    if (null == partitionCountConf) {
      // By default attempt to make partitions the same size
      val maxSplitBytes = if (null == partitionSizeConf) S3InputFormat.DEFAULT_PARTITION_BYTES else partitionSizeConf.toLong

      /**
        * Divide the problem harmonically and apply the NF ("Next
        * Fit") strategy to each sub-part[1].
        *
        * 1. Epstein, Leah, Lene M. Favrholdt, and Jens S. Kohrt.
        *    "Comparing online algorithms for bin packing problems."
        *    Journal of Scheduling 15.1 (2012): 13-21.
        */
      var h1 = splits
      var h2: Vector[S3InputSplit] = Vector(makeNewSplit)
      var h3: Vector[S3InputSplit] = Vector(makeNewSplit)
      val h1Cutoff = maxSplitBytes / 2
      val h2Cutoff = maxSplitBytes / 3

      logger.info(s"Building partitions, attempting to create them with size at most $maxSplitBytes bytes")

      s3client
        .listObjectsV2Paginator(request)
        .contents
        .asScala
        .filter(s3ObjectIsTiff)
        .foreach { s3obj =>
          val curSplit =
            if (s3obj.size <= h2Cutoff) h3.last
            else if ((h2Cutoff < s3obj.size) && (s3obj.size <= h1Cutoff)) h2.last
            else h1.last
          if (curSplit.getLength == 0)
            curSplit.addKey(s3obj)
          else if (curSplit.size + s3obj.size <= maxSplitBytes)
            curSplit.addKey(s3obj)
          else {
            val newSplit = makeNewSplit
            newSplit.addKey(s3obj)
            if (s3obj.size <= h2Cutoff) h3 = h3 :+ newSplit
            else if ((h2Cutoff < s3obj.size) && (s3obj.size <= h1Cutoff)) h2 = h2 :+ newSplit
            else h1 = h1 :+ newSplit
          }
        }
      splits = (h1 ++ h2 ++ h3).filter(_.getLength >  0)
    } else {
      val partitionCount = partitionCountConf.toInt
      logger.info(s"Building partitions of at most $partitionCount objects")
      val keys =
        s3client
          .listObjectsV2Paginator(request)
          .contents
          .asScala
          .filter(s3ObjectIsTiff)
          .toVector

      val groupCount = math.max(1, keys.length / partitionCount)

      splits = keys
        .grouped(groupCount)
        .map { keys =>
          val split = makeNewSplit
          keys.foreach(split.addKey)
          split
        }.toVector
    }

    (splits: Vector[org.apache.hadoop.mapreduce.InputSplit]).asJava
  }
}

object S3InputFormat {
  final val DEFAULT_PARTITION_BYTES =  256 * 1024 * 1024
  final val DEFAULT_CHUNK_SIZE =  8 * 256 * 256
  final val ANONYMOUS = "s3.anonymous"
  final val AWS_ID = "s3.awsId"
  final val AWS_KEY = "s3.awsKey"
  final val BUCKET = "s3.bucket"
  final val PREFIX = "s3.prefix"
  final val EXTENSIONS = "s3.extensions"
  final val REGION = "s3.region"
  final val PARTITION_COUNT = "s3.partitionCount"
  final val PARTITION_BYTES = "S3.partitionBytes"
  final val CHUNK_SIZE = "s3.chunkSize"
  final val CRS_VALUE = "s3.crs"
  final val CREATE_S3CLIENT = "s3.client"
  final val DELIMITER = "s3.delimiter"

  private val idRx = "[A-Z0-9]{20}"
  private val keyRx = "[a-zA-Z0-9+/]+={0,2}"
  private val slug = "[a-zA-Z0-9-.]+"
  val S3UrlRx = new Regex(s"""s3[an]?://(?:($idRx):($keyRx)@)?($slug)/{0,1}(.*)""", "aws_id", "aws_key", "bucket", "prefix")

  // `S3Client`s are HEAVY. We need to avoid creating them as though they're free
  def setCreateS3Client(job: Job, createClient: () => S3Client): Unit =
    setCreateS3Client(job.getConfiguration, createClient)

  def setCreateS3Client(conf: Configuration, createClient: () => S3Client): Unit =
    conf.setSerialized(CREATE_S3CLIENT, createClient)

  def getS3Client(job: JobContext): S3Client = {
    val conf = job.getConfiguration

    val maybeRegion = {
      val r = conf.get(REGION, null)
      if (r != null) Some(Region.of(r))
      else None
    }
    conf.getSerializedOption[() => S3Client](CREATE_S3CLIENT) match {
      case Some(createS3Client) =>
        createS3Client()
      case None =>
        // https://github.com/aws/aws-sdk-java-v2/blob/master/docs/BestPractices.md#reuse-sdk-client-if-possible
        maybeRegion match {
          case Some(region) =>
            S3Client.builder()
              .region(region)
              .build()
          case None =>
            S3ClientProducer.get()
        }
    }
  }

  def removeCreateS3Client(conf: Configuration): Unit =
    conf.unset(CREATE_S3CLIENT)

  /** Set S3N url to use, may include AWS Id and Key */
  def setUrl(job: Job, url: String): Unit =
    setUrl(job.getConfiguration, url)

  def setUrl(conf: Configuration, url: String): Unit = {
    val S3UrlRx(id, key, bucket, prefix) = url

    if (id != null && key != null) {
      conf.set(AWS_ID, id)
      conf.set(AWS_KEY, key)
    }
    conf.set(BUCKET, bucket)
    conf.set(PREFIX, prefix)
  }

  def removeUrl(conf: Configuration): Unit = {
    conf.unset(AWS_ID)
    conf.unset(AWS_KEY)
    removeBucket(conf)
    removePrefix(conf)
  }

  def setBucket(job: Job, bucket: String): Unit =
    setBucket(job.getConfiguration, bucket)

  def setBucket(conf: Configuration, bucket: String): Unit =
    conf.set(BUCKET, bucket)

  def removeBucket(conf: Configuration): Unit =
    conf.unset(BUCKET)

  def setPrefix(job: Job, prefix: String): Unit =
    setPrefix(job.getConfiguration, prefix)

  def setPrefix(conf: Configuration, prefix: String): Unit =
    conf.set(PREFIX, prefix)

  def removePrefix(conf: Configuration): Unit =
    conf.unset(PREFIX)

  /** Set desired partition count */
  def setPartitionCount(job: Job, limit: Int): Unit =
    setPartitionCount(job.getConfiguration, limit)

  /** Set desired partition count */
  def setPartitionCount(conf: Configuration, limit: Int): Unit =
    conf.set(PARTITION_COUNT, limit.toString)

  /** Removes partition count */
  def removePartitionCount(conf: Configuration): Unit =
    conf.unset(PARTITION_COUNT)

  def setRegion(job: Job, region: String): Unit =
    setRegion(job.getConfiguration, region)

  def setRegion(conf: Configuration, region: String): Unit =
    conf.set(REGION, region)

  def removeRegion(conf: Configuration): Unit =
    conf.unset(REGION)

  /** Force anonymous access, bypass all key discovery */
  def setAnonymous(job: Job): Unit =
    setAnonymous(job.getConfiguration)

  /** Force anonymous access, bypass all key discovery */
  def setAnonymous(conf: Configuration): Unit =
    conf.set(ANONYMOUS, "true")

  def removeAnonymous(conf: Configuration): Unit =
    conf.unset(ANONYMOUS)

  /** Set desired partition size in bytes, at least one item per partition will be assigned */
  def setPartitionBytes(job: Job, bytes: Long): Unit =
    setPartitionBytes(job.getConfiguration, bytes)

  /** Set desired partition size in bytes, at least one item per partition will be assigned */
  def setPartitionBytes(conf: Configuration, bytes: Long): Unit =
    conf.set(PARTITION_BYTES, bytes.toString)

  /** Removes partition size in bytes */
  def removePartitionBytes(conf: Configuration): Unit =
    conf.unset(PARTITION_BYTES)

  def setChunkSize(job: Job, chunkSize: Int): Unit =
    setChunkSize(job.getConfiguration, chunkSize)

  def setChunkSize(conf: Configuration, chunkSize: Int): Unit =
    conf.set(CHUNK_SIZE, chunkSize.toString)

  def removeChunkSize(conf: Configuration): Unit =
    conf.unset(CHUNK_SIZE)

  /** Set valid key extensions filter */
  def setExtensions(conf: Configuration, extensions: Seq[String]): Unit =
    conf.set(EXTENSIONS, extensions.mkString(","))

  def removeExtensions(conf: Configuration): Unit =
    conf.unset(EXTENSIONS)

  /** Set delimiter for S3 object listing requests */
  def setDelimiter(job: Job, delimiter: String): Unit =
    setDelimiter(job.getConfiguration, delimiter)

  /** Set delimiter for S3 object listing requests */
  def setDelimiter(conf: Configuration, delimiter: String): Unit =
    conf.set(DELIMITER, delimiter)

  def getDelimiter(job: JobContext): Option[String] =
    getDelimiter(job.getConfiguration)

  def getDelimiter(conf: Configuration): Option[String] =
    Option(conf.get(DELIMITER))

  def removeDelimiter(conf: Configuration): Unit =
    conf.unset(DELIMITER)
}
