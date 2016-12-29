package geotrellis.spark.pointcloud.pipeline.json

import geotrellis.spark.pointcloud.pipeline._
import io.circe.{Json, Encoder, ObjectEncoder}
import io.circe.generic.extras._
import io.circe.generic.extras.auto._
import io.circe.generic.extras.semiauto._
import io.circe.syntax._

object Implicits extends Implicits

trait Implicits {
  implicit val customConfig: Configuration =
    Configuration.default.withSnakeCaseKeys.withDiscriminator("class_type")

  implicit val readerTypeEncoder: Encoder[ReaderType] = Encoder.instance { _.toString.asJson }
  implicit val filterTypeEncoder: Encoder[FilterType] = Encoder.instance { _.toString.asJson }

  /*implicit val reprojectEncoder = {
    val encoder = deriveEncoder[Reproject]
    encoder.mapJson { json => val res = json.deepMerge(Json.obj("type" -> "filters.reproject".asJson)); println(json); json }
  }

  implicit val mergeEncoder = {
    val encoder: ObjectEncoder[Merge] = deriveEncoder[Merge]
    encoder.mapJson(_.deepMerge(Json.obj("type" -> "filters.merge".asJson)))
  }*/
}
