/*
 * Copyright (c) 2016 Azavea.
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

package geotrellis.raster


trait ArrayMultiBandTileSubsetMethod[T <: ArrayMultiBandTile] extends MethodExtensions[T] {

  def subset(m: Map[Int, Int]): ArrayMultiBandTile = {
    val oldBandCount = self.bandCount
    val newBandCount = m.size
    val bands = Array.ofDim[Tile](newBandCount)

    assert(m.keys.toSet == List.range(0, newBandCount).toSet, "Indices in domain of mapping must be contiguous and start at 0")
    m.values.foreach({ i => assert(0 <= i && i < oldBandCount, "Indices in range of mapping must be < bandCount of source") })

    (0 until newBandCount)
      .foreach({ i =>
        val j = m.getOrElse(i, -1)
        bands(i) = self.band(j) })

    new ArrayMultiBandTile(bands)
  }
}
