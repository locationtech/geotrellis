package geotrellis.spark.tiling

object TmsTiling {

  val Epsilon = 0.00000001
  val MaxZoomLevel = 22
  val DefaultTileSize = 512

  def numXTiles(zoom: Int) = math.pow(2, zoom)
  def numYTiles(zoom: Int) = math.pow(2, zoom - 1)

  def tileId(tx: Long, ty: Long, zoom: Int) = (ty * numXTiles(zoom)) + tx

  def resolution(zoom: Int, tileSize: Int) = 360 / (numXTiles(zoom) * tileSize)

  def zoom(res: Double, tileSize: Int) = {
    val resWithEp = res + Epsilon;

    // TODO - avoid materializing array for sake of first element, do that without loop breaks
    val zoom = for (i <- 1 to MaxZoomLevel; if (resWithEp >= resolution(i, tileSize))) yield i
    zoom(0)
  }

  // using equations 2.3 through 2.6 from TBGIS book
  def tileToBounds(tx: Long, ty: Long, zoom: Int, tileSize: Int) = {
    val res = resolution(zoom, tileSize)
    new Bounds(tx * tileSize * res - 180, // left/west (lon, x)
      ty * tileSize * res - 90, // lower/south (lat, y)
      (tx + 1) * tileSize * res - 180, // right/east (lon, x)
      (ty + 1) * tileSize * res - 90) // upper/north (lat, y)
  }

  def latLonToPixels(lat: Double, lon: Double, zoom: Int, tileSize: Int) = {
    val res = resolution(zoom, tileSize)

    new Pixel(((180 + lon) / res).toLong,
      ((90 + lat) / res).toLong)
  }

  def pixelsToTile(px: Double, py: Double, tileSize: Int) = {
    new Tile((px / tileSize).toLong,
      (py / tileSize).toLong)
  }

  // slightly modified version of equations 2.9 and 2.10
  def latLonToTile(lat: Double, lon: Double, zoom: Int, tileSize: Int) = {
    val tx = ((180 + lon) * (numXTiles(zoom) / 360)).toLong
    val ty = ((90 + lat) * (numYTiles(zoom) / 180)).toLong
    new Tile(tx, ty)

    //val pixel = latLonToPixels(lat,lon,zoom,tileSize)
    //pixelsToTile(pixel.px, pixel.py, tileSize)
  }

  def main(args: Array[String]) = {
  }
}