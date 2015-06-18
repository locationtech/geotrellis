package geotrellis.spark.io.parquet

import geotrellis.spark._
import geotrellis.raster._
import geotrellis.spark.utils.KryoSerializer
import geotrellis.spark.io.index._
import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD
import java.io.ByteArrayInputStream
import com.amazonaws.services.s3.model.{ PutObjectRequest, PutObjectResult }
import com.amazonaws.services.s3.model.ObjectMetadata
import com.typesafe.scalalogging.slf4j._
import com.amazonaws.services.s3.model.AmazonS3Exception
import scala.reflect.ClassTag
import java.util.concurrent.Executors
import scalaz.stream._
import scalaz.concurrent.Task

import org.apache.spark.sql.{SQLContext, Row}
import org.apache.spark.sql.types._


private[parquet] abstract class RasterRDDWriter[K: Boundable: ClassTag] extends LazyLogging {

  val createTileRow: (LayerId,  K,  Long,  Array[Byte]) => Row

  def write(
    outPath: String,
    keyIndex: KeyIndex[K],
    clobber: Boolean
  )(layerId: LayerId, rdd: RasterRDD[K])(implicit sc: SparkContext): Unit = {
    val sqlContext = new SQLContext(sc)

    val ctr = createTileRow

    val schema = StructType(Seq(
      StructField("zoomLevel", IntegerType, false),
      StructField("layerName", StringType, false),
      StructField("tileRow", IntegerType, false),
      StructField("tileCol", IntegerType, false),
      StructField("timeMillis", LongType, false),
      StructField("index", LongType, false),
      StructField("tileBytes", ArrayType(ByteType, false), false)
    ))

    val rowRDD: RDD[Row] = rdd.map{case (key, tile) =>
      val bytes = KryoSerializer.serialize[Tile](tile)
      val index = keyIndex.toIndex(key)
      ctr(layerId, key, index, bytes)
    }

    val rasterDataFrame = sqlContext.createDataFrame(rowRDD, schema)

    rasterDataFrame.write.partitionBy("zoomLevel", "layerName").mode("append").parquet(outPath)

  }
}
