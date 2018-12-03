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

object Version {
  val geotrellis  = "3.0.0" + Environment.versionSuffix
  val scala       = "2.11.12"
  val crossScala  = Seq(scala, "2.12.7")
  val geotools    = "20.0"
  val sprayJson   = "1.3.4"
  val monocle     = "1.5.1-cats"
  val spire       = "0.13.0"
  val accumulo    = "1.9.2"
  val cassandra   = "3.6.0"
  val hbase       = "2.1.0"
  val geomesa     = "2.0.2"
  val geowave     = "0.9.3"
  val circe       = "0.10.0"
  val previousVersion = "1.1.0"
  lazy val hadoop = Environment.hadoopVersion
  lazy val spark  = Environment.sparkVersion
}
