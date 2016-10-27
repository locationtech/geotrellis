# Examples

### Counting points in raster cells (raster to vector operation)

Given a set of points, say we want to count them into the cells of some RasterExtent. The following code will do that:

```scala
def countPoints(points: Seq[Point], rasterExtent: RasterExtent): Tile = {
  val (cols, rows) = (rasterExtent.cols, rasterExtent.rows)
  val array = Array.ofDim[Int](cols * rows).fill(0)
  for(point <- points) {
    val x = point.x
    val y = point.y
    if(rasterExtent.extent.intersects(x,y)) {
      val index = rasterExtent.mapXToGrid(x) * cols + rasterExtent.mapYToGrid(y)
      array(index) = array(index) + 1
    }
  }
  IntArrayTile(array, cols, rows)
}
```
