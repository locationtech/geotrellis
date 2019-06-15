/*
 * Copyright 2018 Azavea
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

package geotrellis.spark.store.file.geotiff

import geotrellis.spark.store.hadoop.geotiff._
import geotrellis.util.annotations.experimental

import org.apache.hadoop.conf.Configuration
import spray.json._
import spray.json.DefaultJsonProtocol._

import java.io.PrintWriter
import java.net.URI

/**
  * @define experimental <span class="badge badge-red" style="float: right;">EXPERIMENTAL</span>@experimental
  */
@experimental object FileIMGeoTiffAttributeStore {
  def apply(
    name: String,
    uri: URI
  ): InMemoryGeoTiffAttributeStore =
    new InMemoryGeoTiffAttributeStore {
      lazy val metadataList = HadoopGeoTiffInput.list(name, uri, new Configuration())
      def persist(uri: URI): Unit = {
        val str = metadataList.toJson.compactPrint
        new PrintWriter(uri.toString) { write(str); close }
      }
    }
}
