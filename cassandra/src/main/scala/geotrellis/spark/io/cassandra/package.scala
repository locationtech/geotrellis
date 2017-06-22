package geotrellis.spark.io

import java.math.BigInteger

package object cassandra {
  implicit def bigToBig(i: BigInt): BigInteger = {
    new BigInteger(i.toByteArray)
  }
}
