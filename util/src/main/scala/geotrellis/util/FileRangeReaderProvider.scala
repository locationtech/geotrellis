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

import java.net.URI
import java.nio.file.Paths

class FileRangeReaderProvider extends RangeReaderProvider {
  def canProcess(uri: URI): Boolean = uri.getScheme match {
    case str: String => if (str.toLowerCase == "file") true else false
    case null => true // assume that the user is passing in the path to the catalog
  }

  def rangeReader(uri: URI): FileRangeReader = {
    // Paths.get has certain restrictions on how URIs that are
    // passed to it are formatted. This sometimes prevents
    // URIs that are correctly formatted from being used. To
    // get around this, we will pass in the URI as a String
    // instead without its Scheme (if it had one).
    val isWindows = System.getProperty("os.name").toLowerCase().startsWith("win")
    val targetPath: String = {
      val uriString = uri.toString

      if (uriString.startsWith("file://")) {
        val from = if (isWindows) "file:///" else "file://"
        uriString.slice(from.size, uriString.size + 1)
      } else if (uriString.startsWith("file:")) {
        val from = if (isWindows) "file:/" else "file:"
        uriString.slice(from.size, uriString.size + 1)
      } else
        uriString
    }

    FileRangeReader(Paths.get(targetPath).toFile)
  }
}
