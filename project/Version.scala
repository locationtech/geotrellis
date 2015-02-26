/*
 * Copyright (c) 2014 Azavea.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import scala.util.Properties

object Version {
  def either(environmentVariable: String, default: String): String =
    Properties.envOrElse(environmentVariable, default)

  val geotrellis  = "0.10.0-SNAPSHOT"
  val scala       = "2.11.5"
  val geotools    = "11.0"
  val akka        = "2.3.9"
  val spray       = "1.3.2"
  val jackson     = "1.6.1"
  val monocle     = "1.0.1"
  lazy val hadoop              = either("SPARK_HADOOP_VERSION", "2.5.0")
  lazy val spark               = either("SPARK_VERSION", "1.2.0")
  lazy val cassandra_connector = "1.2.0-alpha2"

}
