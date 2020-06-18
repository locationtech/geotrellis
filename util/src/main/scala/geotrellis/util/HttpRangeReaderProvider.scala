/*
 * Copyright 2019 Azavea
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

import java.net.{URI, URL}

class HttpRangeReaderProvider extends RangeReaderProvider {
  def canProcess(uri: URI): Boolean =
    try {
      val scheme = uri.getScheme
      if (scheme == "http" || scheme == "https") {
        new URL(uri.toString)
        true
      } else {
        false
      }
    } catch {
      case _: Throwable => false
    }

  def rangeReader(uri: URI): HttpRangeReader =
    HttpRangeReader(uri)
}
