package geotrellis.pointcloud.pipeline

sealed trait ReaderType {
  val name: String

  override def toString = name
}

case object bpf extends ReaderType { val name = "readers.bpf" }
case object buffer extends ReaderType { val name = "readers.buffer" }
case object faux extends ReaderType { val name = "readers.faux" }
case object gdal extends ReaderType { val name = "readers.gdal" }
case object geowave extends ReaderType { val name = "readers.geowave" }
case object greyhound extends ReaderType { val name = "readers.greyhound" }
case object ilvis2 extends ReaderType { val name = "readers.ilvis2" }
case object las extends ReaderType { val name = "readers.las" }
case object mrsid extends ReaderType { val name = "readers.mrsid" }
case object nitf extends ReaderType { val name = "readers.nitf" }
case object oci extends ReaderType { val name = "readers.oci" }
case object optech extends ReaderType { val name = "readers.optech" }
case object pcd extends ReaderType { val name = "readers.pcd" }
case object pgpointcloud extends ReaderType { val name = "readers.pgpointcloud" }
case object ply extends ReaderType { val name = "readers.ply" }
case object pts extends ReaderType { val name = "readers.pts" }
case object qfit extends ReaderType { val name = "readers.qfit" }
case object rxp extends ReaderType { val name = "readers.rxp" }
case object sbet extends ReaderType { val name = "readers.sbet" }
case object sqlite extends ReaderType { val name = "readers.sqlite" }
case object txt extends ReaderType { val name = "readers.txt" }
case object tindex extends ReaderType { val name = "readers.tindex" }
case object terrasolid extends ReaderType { val name = "readers.terrasolid" }
case object icebridge extends ReaderType { val name = "readers.icebridge" }

object ReaderTypes {
  lazy val all = List(
    bpf, buffer, faux, gdal, geowave, greyhound, ilvis2, las, mrsid, nitf, oci,
    optech, pcd, pgpointcloud, ply, pts, qfit, rxp, sbet, sqlite, txt, tindex,
    terrasolid, icebridge
  )
}