package geotrellis.spark.io.file

import geotrellis.spark.io.cog.COGBackend

package object cog extends Implicits {
  // Type for ValueReader mixin
  trait FileCOGBackend extends COGBackend
}
