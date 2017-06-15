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

package geotrellis.spark.io.file

import geotrellis.spark.io.index._

import java.io.File

object KeyPathGenerator {
  def apply[K](catalogPath: String, layerPath: String, keyIndex: KeyIndex[K], maxWidth: Int): K => String = {
    val f = new File(catalogPath, layerPath)
    (key: K) => new File(f, Index.encode(keyIndex.toIndex(key), maxWidth)).getAbsolutePath
  }

  def apply(catalogPath: String, layerPath: String, maxWidth: Int): BigInt => String = {
    val f = new File(catalogPath, layerPath)
    (index: BigInt) => new File(f, Index.encode(index, maxWidth)).getAbsolutePath
  }
}
