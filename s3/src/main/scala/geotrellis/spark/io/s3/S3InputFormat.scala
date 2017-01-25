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

package geotrellis.spark.io.s3

import geotrellis.proj4.CRS
import geotrellis.spark.io.hadoop._

import com.amazonaws.services.s3.model.{ListObjectsRequest, ObjectListing}
import com.amazonaws.auth._
import com.amazonaws.regions._
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.mapreduce.{InputFormat, Job, JobContext}
import com.typesafe.scalalogging.LazyLogging

import scala.util.matching.Regex

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
    import scala.collection.JavaConversions._

    val conf = context.getConfiguration
    val anon = conf.get(ANONYMOUS)
    val id = conf.get(AWS_ID)
    val key = conf.get(AWS_KEY)
    val bucket = conf.get(BUCKET)
    val prefix = conf.get(PREFIX)
    val crs = conf.get(CRS_VALUE)
    val region: Option[Region] = {
      val r = conf.get(REGION, null)
      if(r != null) Some(Region.getRegion(Regions.fromName(r)))
      else None
    }

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

    val partitionCountConf = conf.get(PARTITION_COUNT)
    val partitionSizeConf = conf.get(PARTITION_BYTES)
    require(null == partitionCountConf || null == partitionSizeConf,
      "Either PARTITION_COUNT or PARTITION_SIZE option may be set")

    val s3client: S3Client = getS3Client(context)
    for (r <- region) s3client.setRegion(r)

    logger.info(s"Listing Splits: bucket=$bucket prefix=$prefix")

    val request = new ListObjectsRequest()
      .withBucketName(bucket)
      .withPrefix(prefix)

    def makeNewSplit =  {
      val split = new S3InputSplit
      split.bucket = bucket
      split
    }

    var splits: Vector[S3InputSplit] = Vector(makeNewSplit)

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
        .listObjectsIterator(request)
        .filter(!_.getKey.endsWith("/"))
        .filter { obj =>
          if (extensions.isEmpty)
            true
          else {
            val key = obj.getKey
            extensions.map(key.endsWith).reduce(_ || _)
          }
        }
        .foreach { obj =>
          val objSize = obj.getSize
          val curSplit =
            if (objSize <= h2Cutoff) h3.last
            else if ((h2Cutoff < objSize) && (objSize <= h1Cutoff)) h2.last
            else h1.last
          if (curSplit.getLength == 0)
            curSplit.addKey(obj)
          else if (curSplit.size + objSize <= maxSplitBytes)
            curSplit.addKey(obj)
          else {
            val newSplit = makeNewSplit
            newSplit.addKey(obj)
            if (objSize <= h2Cutoff) h3 = h3 :+ newSplit
            else if ((h2Cutoff < objSize) && (objSize <= h1Cutoff)) h2 = h2 :+ newSplit
            else h1 = h1 :+ newSplit
          }
        }
      splits = (h1 ++ h2 ++ h3).filter(_.getLength >  0)
    } else {
      val partitionCount = partitionCountConf.toInt
      logger.info(s"Building partitions of at most $partitionCount objects")
      val keys =
        s3client
          .listObjectsIterator(request)
          .filter(! _.getKey.endsWith("/"))
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

    splits
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

  private val idRx = "[A-Z0-9]{20}"
  private val keyRx = "[a-zA-Z0-9+/]+={0,2}"
  private val slug = "[a-zA-Z0-9-]+"
  val S3UrlRx = new Regex(s"""s3[an]?://(?:($idRx):($keyRx)@)?($slug)/{0,1}(.*)""", "aws_id", "aws_key", "bucket", "prefix")

  def setCreateS3Client(job: Job, createClient: () => S3Client): Unit =
    setCreateS3Client(job.getConfiguration, createClient)

  def setCreateS3Client(conf: Configuration, createClient: () => S3Client): Unit =
    conf.setSerialized(CREATE_S3CLIENT, createClient)

  def getS3Client(job: JobContext): S3Client =
    job.getConfiguration.getSerializedOption[() => S3Client](CREATE_S3CLIENT) match {
      case Some(createS3Client) => createS3Client()
      case None => S3Client.DEFAULT
    }

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

  def setBucket(job: Job, bucket: String): Unit =
    setBucket(job.getConfiguration, bucket)

  def setBucket(conf: Configuration, bucket: String): Unit =
    conf.set(BUCKET, bucket)

  def setPrefix(job: Job, prefix: String): Unit =
    setPrefix(job.getConfiguration, prefix)

  def setPrefix(conf: Configuration, prefix: String): Unit =
    conf.set(PREFIX, prefix)

  /** Set desired partition count */
  def setPartitionCount(job: Job, limit: Int): Unit =
    setPartitionCount(job.getConfiguration, limit)

  /** Set desired partition count */
  def setPartitionCount(conf: Configuration, limit: Int): Unit =
    conf.set(PARTITION_COUNT, limit.toString)

  def setRegion(job: Job, region: String): Unit =
    setRegion(job.getConfiguration, region)

  def setRegion(conf: Configuration, region: String): Unit =
    conf.set(REGION, region)

  /** Force anonymous access, bypass all key discovery */
  def setAnonymous(job: Job): Unit =
    setAnonymous(job.getConfiguration)

  /** Force anonymous access, bypass all key discovery */
  def setAnonymous(conf: Configuration): Unit =
    conf.set(ANONYMOUS, "true")

  /** Set desired partition size in bytes, at least one item per partition will be assigned */
  def setPartitionBytes(job: Job, bytes: Long): Unit =
    setPartitionBytes(job.getConfiguration, bytes)

  /** Set desired partition size in bytes, at least one item per partition will be assigned */
  def setPartitionBytes(conf: Configuration, bytes: Long): Unit =
    conf.set(PARTITION_BYTES, bytes.toString)

  def setChunkSize(job: Job, chunkSize: Int): Unit =
    setChunkSize(job.getConfiguration, chunkSize)

  def setChunkSize(conf: Configuration, chunkSize: Int): Unit =
    conf.set(CHUNK_SIZE, chunkSize.toString)

  /** Set valid key extensions filter */
  def setExtensions(conf: Configuration, extensions: Seq[String]): Unit =
    conf.set(EXTENSIONS, extensions.mkString(","))
}
