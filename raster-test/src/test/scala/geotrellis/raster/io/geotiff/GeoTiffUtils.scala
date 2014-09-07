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

package geotrellis.raster.io.geotiff

import java.io.File

import org.scalatest._

trait GeoTiffTestUtils extends Matchers {

  val epsilon = 1e-9

  var writtenFiles = Vector[String]()

  protected def addToPurge(path: String) = synchronized {
    writtenFiles = writtenFiles :+ path
  }

  protected def purge = writtenFiles foreach { path =>
    val file = new File(path)
    if (file.exists()) file.delete()
  }

  protected def proj4StringToMap(proj4String: String) = proj4String.dropWhile(_ == '+')
    .split('+').map(_.trim)
    .groupBy(_.takeWhile(_ != '='))
    .map(x => (x._1 -> x._2(0)))
    .map { case (a, b) => (a, b.split('=')(1)) }

  protected def compareValues(
    firstMap: Map[String, String],
    secondMap: Map[String, String],
    key: String,
    isDouble: Boolean,
    eps: Double = epsilon) = firstMap.get(key) match {
    case Some(str1) => secondMap.get(key) match {
      case Some(str2) =>
        if (isDouble) math.abs(str1.toDouble - str2.toDouble) should be <= eps
        else str1 should equal (str2)
      case None => fail
    }
    case None => fail
  }

}
