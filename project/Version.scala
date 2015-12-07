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
  val geotrellis  = "0.10.0" + Environment.versionSuffix
  /* Even though we support cross-build to 2.11 the default target is scala 2.10 primarily because Cloudera
   * (and likely others) spark distributions target 2.10 in their default spark-assembly.jar.
   * One can envoke the cross-build to 2.11 by prefixing command with '+' (ex: + assembly)
   * Until the deployment of spark on 2.11 is fully addressed we are going to target 2.10 to minimize confusion.
   */
  val scala       = "2.10.4"
  val crossScala  = Seq("2.11.5", "2.10.4")
  val geotools    = "13.1"
  val akka        = "2.3.9"
  val spray       = "1.3.3"
  val sprayJson   = "1.3.2"
  val jackson     = "1.6.1"
  val monocle     = "1.0.1"
  val accumulo    = "1.6.3"
  lazy val hadoop      = Environment.hadoopVersion
  lazy val spark       = Environment.sparkVersion
}
