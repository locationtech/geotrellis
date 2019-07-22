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
import java.io.File


class FileRangeReaderProvider extends RangeReaderProvider {
  def canProcess(uri: URI): Boolean = uri.getScheme match {
    case str: String => if (str.toLowerCase == "file") true else false
    case null => true // assume that the user is passing in the path to the catalog
  }

  def rangeReader(uri: URI): FileRangeReader = {
    val containsScheme: Boolean =
      uri.getScheme match {
        case _: String => true
        case null => false
      }

    val fileSchemeSize: Int = "file://".size

    // Certain systems (like Linux) do not allow URIs to have
    // Authorities when being passed to Paths.get, but others do.
    // In order to simplify things, all URIs given will
    // be formatted as a String and then passed to Paths.
    val targetPath: String =
      if (containsScheme)
        uri.toString.slice(fileSchemeSize, uri.toString.size + 1)
      else
        uri.toString

    FileRangeReader(Paths.get(targetPath).toFile)
  }
}
