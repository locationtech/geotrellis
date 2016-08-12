package geotrellis.spark.etl.config

case class Backend(`type`: BackendType, path: BackendPath, profile: Option[BackendProfile] = None)
