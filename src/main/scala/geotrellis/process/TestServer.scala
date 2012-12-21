package geotrellis.process

object TestServer {
  def apply() = Server("test", Catalog.empty("test"))
  def apply(path:String) = new Server("test", Catalog.fromPath(path))
}
