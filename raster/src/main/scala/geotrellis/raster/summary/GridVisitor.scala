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

package geotrellis.raster.summary

import geotrellis.raster.Grid

/**
  * Visitor used to summarize a two-dimensional grid T that stores values of R
  *
  * The user should implement concrete subclasses that update the value of `result` as
  * necessary on each call to `visit(raster: T, col: Int, row: Int)`.
  *
  * Be sure to handle the empty state. This could occur if no points in T are ever visited.
  *
  * GridVisitor is contravariant in T and covariant in R in the same fashion as Function1.
  * This allows more generic concrete Visitor class implementations
  * to satisfy type constraints for T at the call site, and the inverse is true for R. As a
  * contrived example using GeoTrellis types, where Raster is a subtype of CellGrid:
  *
  * val DoubleVisitor: GridVisitor[CellGrid[Int], Double] = ???
  * def usesRaster(visitor: GridVisitor[Raster[Tile]], Any]): Unit = ???
  * usesRaster(DoubleVisitor)
  *
  * will compile. For an alternate explanation on covariance and contravariance that might help
  * to clarify this, see:
  * - https://stackoverflow.com/a/10604305
  * - https://stackoverflow.com/a/38577878
  */
trait GridVisitor[-T <: Grid[Int], +R] {

  def result: R

  /**
    * Called when the visitor requests a unit of computation for the given col and row.
    *
    * The visitor result should be updated within this method as appropriate for
    * the implementation.
    *
    * @see [[polygonal.visitors.TileFoldingVisitor]] and [[polygonal.visitors.MaxVisitor]] for an example concrete implementation.
    *
    * @param grid The grid being visited
    * @param col The column in the grid being visited
    * @param row The row in the grid being visited
    */
  def visit(grid: T, col: Int, row: Int): Unit
}

object GridVisitor {
  def apply[T <: Grid[Int], R](implicit ev: GridVisitor[T, R]): GridVisitor[T, R] = ev
}
