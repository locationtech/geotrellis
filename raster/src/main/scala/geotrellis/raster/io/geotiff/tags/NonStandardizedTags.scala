package geotrellis.raster.io.geotiff.tags

import monocle.macros.Lenses

import scala.collection.immutable.HashMap

@Lenses("_")
case class NonStandardizedTags(
  asciisMap: HashMap[Int, String] = HashMap[Int, String](),
  longsMap: HashMap[Int, Array[Long]] = HashMap[Int, Array[Long]](),
  fractionalsMap: HashMap[Int, Array[(Long, Long)]] = HashMap[Int, Array[(Long, Long)]](),
  undefinedMap: HashMap[Int, Array[Byte]] = HashMap[Int, Array[Byte]](),
  doublesMap: HashMap[Int, Array[Double]] = HashMap[Int, Array[Double]]()
)
