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

package geotrellis.spark.pointcloud

import spray.json._
import spray.json.DefaultJsonProtocol._

import java.io.File
import scala.collection.mutable

package object json extends MetadataFormat {
  def getPipelineJson(localPath: File, targetCrs: Option[String] = None, additionalSteps: Seq[JsObject] = Seq()): JsObject = {
    val pipeline = mutable.ListBuffer[JsObject]()
    pipeline += JsObject( "filename" -> localPath.getAbsolutePath.toJson )
    targetCrs.foreach { crs =>
      pipeline += JsObject(
        "type" -> "filters.reprojection".toJson,
        "out_srs" -> crs.toJson
      )
    }
    pipeline ++= additionalSteps

    JsObject( "pipeline" -> JsArray(pipeline.toVector) )
  }
}
