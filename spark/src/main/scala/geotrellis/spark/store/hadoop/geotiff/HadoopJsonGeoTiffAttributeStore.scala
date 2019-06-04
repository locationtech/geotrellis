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

package geotrellis.spark.store.hadoop.geotiff

import geotrellis.util.annotations.experimental

import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.Path
import spray.json._
import spray.json.DefaultJsonProtocol._

import java.net.URI
import java.io.PrintWriter

import scala.io.Source

/**
  * @define experimental <span class="badge badge-red" style="float: right;">EXPERIMENTAL</span>@experimental
  */
@experimental object HadoopJsonGeoTiffAttributeStore {
  @experimental def readData(uri: URI, conf: Configuration): List[GeoTiffMetadata] = {
    val path = new Path(uri)
    val fs = path.getFileSystem(conf)
    val stream = fs.open(path)
    val json = try {
      Source
        .fromInputStream(stream)
        .getLines
        .mkString(" ")
    } finally stream.close()

    json
      .parseJson
      .convertTo[List[GeoTiffMetadata]]
  }

  @experimental def readDataAsTree(uri: URI, conf: Configuration): GeoTiffMetadataTree[GeoTiffMetadata] =
    GeoTiffMetadataTree.fromGeoTiffMetadataSeq(readData(uri, conf))

  def apply(uri: URI): JsonGeoTiffAttributeStore =
    JsonGeoTiffAttributeStore(uri, readDataAsTree(_, new Configuration))

  def apply(path: Path, name: String, uri: URI, conf: Configuration): JsonGeoTiffAttributeStore = {
    val data = HadoopGeoTiffInput.list(name, uri, conf)
    val attributeStore = JsonGeoTiffAttributeStore(path.toUri, readDataAsTree(_, conf))
    val fs = path.getFileSystem(conf)

    if(fs.exists(path)) {
      attributeStore
    } else {
      val fdos = fs.create(path)
      val out = new PrintWriter(fdos)
      try {
        val s = data.toJson.prettyPrint
        out.println(s)
      } finally {
        out.close()
        fdos.close()
      }

      attributeStore
    }
  }
}
