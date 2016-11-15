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

package geotrellis.spark.etl

import geotrellis.spark.etl.config.{Backend, BackendProfile, HBasePath, HBaseProfile}
import geotrellis.spark.io.hbase.HBaseInstance

package object hbase {
  private[hbase] def getInstance(bp: Option[BackendProfile]): HBaseInstance =
    bp.collect { case ap: HBaseProfile => ap.getInstance }.get

  def getPath(b: Backend): HBasePath =
    b.path match {
      case p: HBasePath => p
      case _ => throw new Exception("Path string not corresponds backend type")
    }
}
