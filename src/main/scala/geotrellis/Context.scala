package geotrellis

import geotrellis.process._

class Context (server:Server) {
  val timer = new Timer()

  def loadRaster(path:String, g:RasterExtent):Raster = 
    server.getRaster(path, None, Option(g))

  def loadTileSet(path:String):Raster = Raster.loadTileSet(path, server)

  def loadUncachedTileSet(path:String):Raster = Raster.loadUncachedTileSet(path, server)

  def getRaster(path:String, layer:RasterLayer, re:RasterExtent):Raster = 
    server.getRaster(path, Option(layer), Option(re))

  def getRasterStepOutput(path:String, layer:Option[RasterLayer], re:Option[RasterExtent]):StepOutput[Raster] = 
    server.getRasterStepOutput(path, layer, re)

  def getRasterByName(name:String, re:RasterExtent):StepOutput[Raster] = 
    server.getRasterByName(name, Option(re))

  def getRasterExtentByName(name:String):RasterExtent = 
    server.getRasterExtentByName(name)
}
