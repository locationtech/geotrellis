package geotrellis.spark

object TimeTileId {
  implicit object v1 extends Filterable[TimeTileId, TimeFilter]
  implicit object v2 extends Filterable[TimeTileId, SpaceFilter]
}

case class TimeTileId(tileId: TileId, time: Double)
