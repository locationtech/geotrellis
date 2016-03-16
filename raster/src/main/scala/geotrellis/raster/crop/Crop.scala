package geotrellis.raster.crop

/**
  * An object which collects a class and a objected related to
  * cropping options.
  */
object Crop {

  /**
    * Case class encoding cropping options.
    */
  case class Options(
    /**
      * When cropping, clamp the incoming extent or bounds to the
      * source boundaries. If false, the return value might not be
      * contained by the source, and NoData values will be placed into
      * cell values that do not have a corresponding source value.
      */
    clamp: Boolean = true,

    /**
      * When cropping, if force is true, an new [[ArrayTile]] will be
      * created for the result tile. If it is false, a lazy cropping
      * method might be used, where the cropped tile does not actually
      * hold values but does the math to translate the tile methods of
      * the cropped tile to the values of the source value.
      */
    force: Boolean = false
  )

  /**
    * The companion object for the [[Options]] type.  Provides a
    * default set of options.
    */
  object Options {
    def DEFAULT = Options()
  }
}
