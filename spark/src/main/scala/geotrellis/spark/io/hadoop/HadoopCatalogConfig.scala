package geotrellis.spark.io.hadoop

import geotrellis.spark._

case class HadoopCatalogConfig(
  /** Compression factor for determining how many tiles can fit into
    * one block on a Hadoop-readable file system. */
  compressionFactor: Double,

  /** Name of the splits file that contains the partitioner data */
  splitsFile: String,

  /** Name of file that will contain the metadata under the layer path. */
  metaDataFileName: String,

  /** Creates a subdirectory path based on a layer id. */
  layerDataDir: LayerId => String
) {
  /** Sequence file data directory for reading data.
    * Don't see a reason why the API would allow this to be modified
    */
  final val SEQFILE_GLOB = "/*[0-9]*/data"
}

object HadoopCatalogConfig {
  val DEFAULT =
    HadoopCatalogConfig(
      compressionFactor = 1.3, // Assume tiles can be compressed 30% (so, compressionFactor - 1)
      splitsFile = "splits",
      metaDataFileName = "metadata.json",
      layerDataDir = { layerId: LayerId => s"${layerId.name}/${layerId.zoom}" }
    )
}
