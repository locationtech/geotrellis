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

    val maxKeys: Integer = {
      val max = conf.get(MAX_KEYS)
      if (max != null)  max.toInt  else  null
    }

    val credentials =
      if (anon != null)
        new AnonymousAWSCredentials()
      else if (id != null && key != null)
        new BasicAWSCredentials(id, key)
      else
        new DefaultAWSCredentialsProviderChain().getCredentials

    val s3client = new com.amazonaws.services.s3.AmazonS3Client(credentials)

    region match {
      case Some(r) =>
        s3client.setRegion(r)
      case None =>
    }

    logger.info(s"Listing Splits: bucket=$bucket prefix=$prefix")
    logger.debug(s"Authenticationg with ID=${credentials.getAWSAccessKeyId}")
    val request = new ListObjectsRequest()
      .withBucketName(bucket)
      .withPrefix(prefix)
      .withMaxKeys(maxKeys)

    var listing: ObjectListing = null
    var splits: List[S3InputSplit] = Nil
    do {
      listing = s3client.listObjects(request)
      val split = new S3InputSplit()
      split.setCredentials(credentials)
      split.bucket = bucket
      // avoid including "directories" in the input split, can cause 403 errors on GET
      split.keys = listing.getObjectSummaries.map(_.getKey).filterNot(_ endsWith "/")

      splits = split :: splits
      request.setMarker(listing.getNextMarker)
    } while (listing.isTruncated)

    splits
  }
}

object S3InputFormat {
  final val ANONYMOUS = "s3.anonymous"
  final val AWS_ID = "s3.awsId"
  final val AWS_KEY = "s3.awsKey"
  final val BUCKET = "s3.bucket"
  final val PREFIX = "s3.prefix"
  final val REGION = "s3.maxKeys"
  final val MAX_KEYS = "s3.region"

  private val idRx = "[A-Z0-9]{20}"
  private val keyRx = "[a-zA-Z0-9+/]+={0,2}"
  private val slug = "[a-z0-9-]+"
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

  /** Set maximum number of keys per split, less may be returned */
  def setMaxKeys(job: Job, limit: Int): Unit =
    setMaxKeys(job.getConfiguration, limit)

  def setMaxKeys(conf: Configuration, limit: Int): Unit =
    conf.set(MAX_KEYS, limit.toString)

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
}
