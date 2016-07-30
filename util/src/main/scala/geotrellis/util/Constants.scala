package geotrellis.util

object Constants {
  /**
   * Defines machine epsilon for double-precision floating point ops.  This is,
   * roughly speaking, the minimum distance between distinct floating point numbers.
   * Double values closer than DOUBLE_EPSILON should be considered identical.
   */
  val DOUBLE_EPSILON = 1.11e-16

  /**
   * Defines machine epsilon for single-precision floating point ops.  This is,
   * roughly speaking, the minimum distance between distinct floating point numbers.
   * Float values closer than FLOAT_EPSILON should be considered identical.
   */
  val FLOAT_EPSILON = 5.96e-8
}
