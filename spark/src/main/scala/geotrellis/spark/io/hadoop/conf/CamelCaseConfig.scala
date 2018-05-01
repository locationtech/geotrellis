package geotrellis.spark.io.hadoop.conf

import pureconfig.{CamelCase, ConfigFieldMapping, ProductHint}

trait CamelCaseConfig {
  implicit def hint[T]: ProductHint[T] = ProductHint[T](ConfigFieldMapping(CamelCase, CamelCase))
}
