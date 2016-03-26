package geotrellis.spark.io.s3

import com.amazonaws.services.s3.model.{ListObjectsRequest, ObjectListing}
import com.amazonaws.auth._
import com.amazonaws.regions._
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.mapreduce.{InputFormat, Job, JobContext}
import com.typesafe.scalalogging.slf4j._

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

  def getS3Client(credentials: AWSCredentials): S3Client =
    new geotrellis.spark.io.s3.AmazonS3Client(credentials, S3Client.defaultConfiguration)

  override def getSplits(context: JobContext) = {
    import scala.collection.JavaConversions._

    val conf = context.getConfiguration
    val anon = conf.get(ANONYMOUS)
    val id = conf.get(AWS_ID)
    val key = conf.get(AWS_KEY)
    val bucket = conf.get(BUCKET)
    val prefix = conf.get(PREFIX)
    val region: Option[Region] = {
      val r = conf.get(REGION, null)
      if(r != null) Some(Region.getRegion(Regions.fromName(r)))
      else None
    }

    val partitionCountConf = conf.get(PARTITION_COUNT)
    val partitionSizeConf = conf.get(PARTITION_BYTES)
    require(null == partitionCountConf || null == partitionSizeConf,
      "Either PARTITION_COUNT or PARTITION_SIZE option may be set")

    val credentials =
      if (anon != null)
        new AnonymousAWSCredentials()
      else if (id != null && key != null)
        new BasicAWSCredentials(id, key)
      else
        new DefaultAWSCredentialsProviderChain().getCredentials

    val s3client: S3Client = getS3Client(credentials)
    for (r <- region) s3client.setRegion(r)

    logger.info(s"Listing Splits: bucket=$bucket prefix=$prefix")
    logger.debug(s"Authenticating with ID=${credentials.getAWSAccessKeyId}")

    val request = new ListObjectsRequest()
      .withBucketName(bucket)
      .withPrefix(prefix)

    def makeNewSplit =  {
      val split = new S3InputSplit
      split.setCredentials(credentials)
      split.bucket = bucket
      split
    }

    var splits: Vector[S3InputSplit] = Vector(makeNewSplit)

    if (null == partitionCountConf) {
      // By default attempt to make partitions the same size
      val maxSplitBytes = if (null == partitionSizeConf) S3InputFormat.DEFAULT_PARTITION_BYTES else partitionSizeConf.toLong
      logger.info(s"Building partitions, attempting to create them with size at most $maxSplitBytes bytes")
      s3client
        .listObjectsIterator(request)
        .filter(!_.getKey.endsWith("/"))
        .foreach { obj =>
          val curSplit = splits.last
          val objSize = obj.getSize
          if (curSplit.getLength == 0)
            curSplit.addKey(obj)
          else if (curSplit.size + objSize <= maxSplitBytes)
            curSplit.addKey(obj)
          else {
            val newSplit = makeNewSplit
            newSplit.addKey(obj)
            splits = splits :+ newSplit
          }
        }
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
  final val ANONYMOUS = "s3.anonymous"
  final val AWS_ID = "s3.awsId"
  final val AWS_KEY = "s3.awsKey"
  final val BUCKET = "s3.bucket"
  final val PREFIX = "s3.prefix"
  final val REGION = "s3.region"
  final val PARTITION_COUNT = "s3.partitionCount"
  final val PARTITION_BYTES = "S3.partitionBytes"

  private val idRx = "[A-Z0-9]{20}"
  private val keyRx = "[a-zA-Z0-9+/]+={0,2}"
  private val slug = "[a-zA-Z0-9-]+"
  val S3UrlRx = new Regex(s"""s3n://(?:($idRx):($keyRx)@)?($slug)/{0,1}(.*)""", "aws_id", "aws_key", "bucket", "prefix")

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
}
