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

package geotrellis.proj4.util

import geotrellis.proj4._

import scala.collection.mutable

object CRSCache {

  val projectionCache = 
    mutable.HashMap[String, CoordinateReferenceSystem]()

  val crsFactory = new CRSFactory()

  def apply(): CRSCache = new CRSCache()

}

class CRSCache {

  import CRSCache._

  def createFromName(name: String): CoordinateReferenceSystem =
    projectionCache.get(name) match {
      case Some(crs) => crs
      case None => {
        val crs = crsFactory.createFromName(name)
        projectionCache += (name -> crs)
        crs
      }
    }

}
