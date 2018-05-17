/*
 * Copyright 2017 Azavea
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

package geotrellis.util

import java.net.URI

object UriUtils {
  /** Parse URI user and password */
  def getUserInfo(uri: URI): (Option[String], Option[String]) = {
    val info = uri.getUserInfo
    if (null == info)
      None -> None
    else {
      val chunk = info.split(":")
      if (chunk.length == 1)
        Some(chunk(0)) -> None
      else
        Some(chunk(0)) -> Some(chunk(1))
    }
  }

  def getParams(uri: URI): Map[String, String] = {
    val query = uri.getQuery
    if (null == query)
      Map.empty[String, String]
    else {
      query.split("&").map{ param =>
        val arr = param.split("=")
        arr(0) -> arr(1)
      }.toMap
    }
  }
}
