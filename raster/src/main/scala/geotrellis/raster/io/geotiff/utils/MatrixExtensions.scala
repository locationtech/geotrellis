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

package geotrellis.raster.io.geotiff.utils

import java.util.BitSet

trait MatrixExtensions {

  implicit class MatrixUtilities(matrix: Array[Array[Double]]) {

    def validateAsMatrix =
      if (matrix.size != 0) {
        var size = matrix(0)
        var i = 1
        while (i < matrix.size)
          if (matrix(i) != size)
            throw new IllegalArgumentException("malformed matrix")
          else i += 1

        true
      } else throw new IllegalArgumentException("empty matrices not allowed")

    def *(second: Array[Array[Double]]): Array[Array[Double]] = {
      matrix.validateAsMatrix
      second.validateAsMatrix

      if (second.size == matrix(0).size) {
        val resX = matrix.size
        val resY = second(0).size
        val res = Array.ofDim[Array[Double]](resX)

        for (i <- 0 until matrix.size) {
          res(i) = Array.ofDim[Double](resY)
          for (j <- 0 until second(0).size) {
            var sum: Double = 0
            for (k <- 0 until second.size) {
              sum += matrix(i)(j) * second(k)(j)
            }

            res(i)(j) = sum
          }
        }

        res
      } else throw new IllegalArgumentException("matrices not fitting together")
    }

  }

}
