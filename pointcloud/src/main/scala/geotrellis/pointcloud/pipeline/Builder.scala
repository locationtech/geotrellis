package geotrellis.pointcloud.pipeline

import geotrellis.pointcloud.pipeline.json._
import io.circe.syntax._

class Builder {
  val obj = Read("path", Some("crs")) ~ ReprojectionFilter("crsreproject") ~ ReprojectionFilter("") ~ Write("")

  obj.mapExpr {
    case read: Read => read.copy(filename = "path2")
    case s => s
  }

  val json = obj.asJson
  val jsonString: String = obj

  object m {
    trait CC {
      val name = this.getClass.getName.split("\\$").last
    }
  }

  // new Pipeline(obj)
}
