package geotrellis.spark.pointcloud.pipeline

import io.circe._
import io.circe.generic.extras._
import io.circe.generic.extras.auto._
import io.circe.syntax._

class Builder {
  import geotrellis.spark.pointcloud.pipeline._
  import geotrellis.spark.pointcloud.pipeline.json._

  val obj = Read("path", Some("crs")) ~ ReprojectionFilter("crsreproject") ~ ReprojectionFilter("") ~ Write("")
  obj.json


  1 :: Nil
}
