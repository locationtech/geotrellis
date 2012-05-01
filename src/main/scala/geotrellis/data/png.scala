package geotrellis.data.png

//final class RgbaEncoder extends Encoder(Settings(Rgba, NoFilter))
final class RgbaEncoder extends Encoder(Settings(Rgba, PaethFilter))

object RgbaEncoder { 
  def apply() = new RgbaEncoder
}

//final class RgbEncoder(bg:Int, trans:Int) extends Encoder(Settings(Rgb(bg, trans), NoFilter))
final class RgbEncoder(bg:Int, trans:Int) extends Encoder(Settings(Rgb(bg, trans), PaethFilter))

object RgbEncoder {
  def apply(bg:Option[Int], trans:Option[Int]) = new RgbEncoder(bg.get, trans.get)
}
