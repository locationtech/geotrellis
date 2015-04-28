package geotrellis.spark.io.s3.spacetime

import geotrellis.spark._
import geotrellis.raster._
import geotrellis.spark.utils.KryoSerializer
import geotrellis.spark.io.index._
import geotrellis.spark.io.s3._
import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._
import java.io.ByteArrayInputStream
import com.amazonaws.services.s3.model.{PutObjectRequest, PutObjectResult}
import com.amazonaws.auth.{AWSCredentialsProvider, BasicAWSCredentials}
import com.amazonaws.services.s3.model.ObjectMetadata
import geotrellis.index.zcurve.Z2
import com.typesafe.scalalogging.slf4j._
import scala.collection.mutable.ArrayBuffer
import com.amazonaws.services.s3.model.AmazonS3Exception
import scala.reflect.ClassTag
import org.joda.time.format.ISODateTimeFormat

object SpaceTimeRasterRDDWriter extends RasterRDDWriter[SpaceTimeKey] with LazyLogging {
  private val fmt = ISODateTimeFormat.dateTime()

  val encodeKey = (key: SpaceTimeKey, ki: KeyIndex[SpaceTimeKey], max: Int) => {
    val index: String = ki.toIndex(key).toString.reverse.padTo(max, '0').reverse
    val isoTime: String = fmt.print(key.time)
    f"${index}-${isoTime}"
  }

  def getKeyBounds(rdd: RasterRDD[SpaceTimeKey]): KeyBounds[SpaceTimeKey] = {
    val boundable = implicitly[Boundable[SpaceTimeKey]]
    rdd
      .map{ case (k, tile) => KeyBounds(k, k) }
      .reduce { boundable.combine }
  }

}