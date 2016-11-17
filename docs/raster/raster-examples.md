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

### Matching two rasters of a different CRS so that you can perform operations between them.

Consider you have two rasters, `r1` and `r2`, that are of a different resolution and CRS.
Say we wanted to take a weighted sum between them over some Extent `e`.
In order to do map algebra between the two rasters, they must match extends and resolution.
We can do this by picking one of the raster's CRS and resolution, and modifying the other
to match it. Here is an example of that:
