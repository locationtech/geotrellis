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

package geotrellis.layers.file.conf

import geotrellis.layers.util.threadsFromString
import geotrellis.util.CamelCaseConfig

import pureconfig.generic.auto._


// TODO: Break out the RDDConfig when we rework how we do threading

case class FileCollectionConfig(read: String = "default") {
  def readThreads: Int = threadsFromString(read)
}
case class FileRDDConfig(read: String = "default") {
  def readThreads: Int = threadsFromString(read)
}

case class FileThreadsConfig(
  collection: FileCollectionConfig = FileCollectionConfig(),
  rdd: FileRDDConfig = FileRDDConfig()
)

case class FileConfig(threads: FileThreadsConfig = FileThreadsConfig())

object FileConfig extends CamelCaseConfig {
  lazy val conf: FileConfig = pureconfig.loadConfigOrThrow[FileConfig]("geotrellis.file")
  implicit def fileConfigToClass(obj: FileConfig.type): FileConfig = conf
}
