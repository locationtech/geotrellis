package geotrellis.vectortile

import java.nio.file.{Files, Paths}

import geotrellis.vector._

/** A representation of a VectorTile. See:
  * https://github.com/mapbox/vector-tile-spec/tree/master/2.1
  *
  * @constructor from the naively decoded protobuff. See:
  *              vectortile/vector_tile/VectorTile.scala
  * @param _vector_tile the naively decoded tile
  */
class VectorTile(val _vector_tile: vector_tile.Tile) {

    val layers: Set[Layer] = _vector_tile.layers
                                         .map(x => new Layer(x))
                                         .toSet

    /** @constructor from a file name.
      * @param filename the filename
      */
    def this(filename: String) {
        // http://stackoverflow.com/questions/7598135/
        this(vector_tile.Tile
                        .parseFrom(Files.readAllBytes(Paths.get(filename))))
    }

    /** A representation of a VectorTile Layer.
      *
      * @constructor from the naively decoded protobuff
      * @param _layer the naively decoded layer
      */
    class Layer(val _layer: vector_tile.Tile.Layer) {

        val version: Int = _layer.version // originally unsigned

        val name: String = _layer.name

        val keys: Seq[String] = _layer.keys

        val vals: Seq[Value] = _layer.values
                                     .map(x => new Value(x))

        val features: Set[Feature] = _layer.features
                                           .map(x => new Feature(x))
                                           .toSet

        val extent: Int = _layer.extent match { // originally unsigned
            case None => 4096 // default
            case Some(ext) => ext
        }

        /** The extracted value from the ``singleton'' tile value.
          *
          * @constructor from a tile value
          * @param _value the tile value
          */
        class Value(val _value: vector_tile.Tile.Value) {

            // doesn't support extensions yet
            val value: Any = List(_value.stringValue,
                                  _value.floatValue,
                                  _value.doubleValue,
                                  _value.intValue,
                                  _value.uintValue,
                                  _value.sintValue,
                                  _value.boolValue).flatten.head
        }

        /** A representation of a VectorTile Feature.
          *
          * @constructor from a naively decoded protobuff
          * @param _feature the naively decoded feature
          */
        class Feature(val _feature: vector_tile.Tile.Feature) {


            val id: Option[Long] = _feature.id

            val tags: Map[String, Value] = pair(_feature.tags)

            val geometry: Geometry =
                Command.parse(_feature.`type`.get, _feature.geometry)

            /**  Pairs off the keys and value tags. */
            private def pair(tags: Seq[Int]): Map[String, Value] = {
                var idx: Int = 0
                var paired: collection.mutable.Map[String, Value] =
                    new collection.mutable.HashMap[String, Value]()
                while (idx < tags.length) {
                    paired += (keys(tags(idx)) -> vals(tags(idx+1)))
                    idx += 2
                }
                return Map(paired.toList: _*)
            }

        }

    }

}

