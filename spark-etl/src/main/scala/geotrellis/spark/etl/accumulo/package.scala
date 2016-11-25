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

import geotrellis.spark.etl.config.{AccumuloPath, AccumuloProfile, Backend, BackendProfile}
import geotrellis.spark.io.accumulo.AccumuloInstance

package object accumulo {
  private[accumulo] def getInstance(bp: Option[BackendProfile]): AccumuloInstance =
    bp.collect { case ap: AccumuloProfile => ap.getInstance }.get

  def getPath(b: Backend): AccumuloPath =
    b.path match {
      case p: AccumuloPath => p
      case _ => throw new Exception("Path string not corresponds backend type")
    }
}
