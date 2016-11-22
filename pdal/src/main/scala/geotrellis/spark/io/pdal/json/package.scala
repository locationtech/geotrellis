package geotrellis.spark.io.pdal

import java.io.File

import spray.json._
import spray.json.DefaultJsonProtocol._

package object json extends MetadataFormat {
  def fileToPipelineJson(localPath: File): JsObject =
    JsObject("pipeline" -> JsArray(JsObject("filename" -> localPath.getAbsolutePath.toJson)))
}
