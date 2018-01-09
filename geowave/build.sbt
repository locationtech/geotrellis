import Dependencies._
import sbtassembly.PathList


name := "geotrellis-geowave"

libraryDependencies ++= Seq(
  "org.apache.accumulo" % "accumulo-core" % Version.accumulo
    exclude("org.jboss.netty", "netty")
    exclude("org.apache.hadoop", "hadoop-client"),
  "mil.nga.giat" % "geowave-adapter-raster" % "0.9.3"
    excludeAll(ExclusionRule(organization = "org.mortbay.jetty"),
      ExclusionRule(organization = "javax.servlet")),
  "mil.nga.giat" % "geowave-adapter-vector" % "0.9.3"
    excludeAll(ExclusionRule(organization = "org.mortbay.jetty"),
      ExclusionRule(organization = "javax.servlet")),
  "mil.nga.giat" % "geowave-core-store" % "0.9.3"
    excludeAll(ExclusionRule(organization = "org.mortbay.jetty"),
      ExclusionRule(organization = "javax.servlet")),
  "mil.nga.giat" % "geowave-core-geotime" % "0.9.3"
    excludeAll(ExclusionRule(organization = "org.mortbay.jetty"),
      ExclusionRule(organization = "javax.servlet")),
  "mil.nga.giat" % "geowave-datastore-accumulo" % "0.9.3"
    excludeAll(ExclusionRule(organization = "org.mortbay.jetty"),
      ExclusionRule(organization = "javax.servlet")),
  hadoopClient % Provided
    excludeAll(ExclusionRule(organization = "org.mortbay.jetty"),
      ExclusionRule(organization = "javax.servlet")),
  "org.geotools" % "gt-coverage" % Version.geotools % Provided
    excludeAll(ExclusionRule(organization = "org.mortbay.jetty"),
      ExclusionRule(organization = "javax.servlet")),
  "org.geotools" % "gt-epsg-hsql" % Version.geotools % Provided
    excludeAll(ExclusionRule(organization = "org.mortbay.jetty"),
      ExclusionRule(organization = "javax.servlet")),
  "org.geotools" % "gt-main" % Version.geotools % Provided
    excludeAll(ExclusionRule(organization = "org.mortbay.jetty"),
      ExclusionRule(organization = "javax.servlet")),
  "org.geotools" % "gt-referencing" % Version.geotools % Provided
    excludeAll(ExclusionRule(organization = "org.mortbay.jetty"),
      ExclusionRule(organization = "javax.servlet")),
  "com.jsuereth" %% "scala-arm" % "2.0",
  "de.javakaffee" % "kryo-serializers" % "0.38" exclude("com.esotericsoftware", "kryo"),
  "com.esotericsoftware" % "kryo-shaded" % "3.0.3",
  sparkCore % Provided,
  spire,
  scalatest % Test
)

resolvers ++= Seq(
  // Resolver.mavenLocal,
  "boundless" at "https://repo.boundlessgeo.com/release",
  "geosolutions" at "http://maven.geo-solutions.it/",
  "geowave-release" at "http://geowave-maven.s3-website-us-east-1.amazonaws.com/release",
  "geowave-snapshot" at "http://geowave-maven.s3-website-us-east-1.amazonaws.com/snapshot",
  "osgeo" at "http://download.osgeo.org/webdav/geotools/"
)

assemblyMergeStrategy in assembly := {
  case "reference.conf" => MergeStrategy.concat
  case "application.conf" => MergeStrategy.concat
  case PathList("META-INF", xs @ _*) =>
    xs match {
      case ("MANIFEST.MF" :: Nil) => MergeStrategy.discard
      // Concatenate everything in the services directory to keep GeoTools happy.
      case ("services" :: _ :: Nil) =>
        MergeStrategy.concat
      // Concatenate these to keep JAI happy.
      case ("javax.media.jai.registryFile.jai" :: Nil) | ("registryFile.jai" :: Nil) | ("registryFile.jaiext" :: Nil) =>
        MergeStrategy.concat
      case (name :: Nil) => {
        // Must exclude META-INF/*.([RD]SA|SF) to avoid "Invalid signature file digest for Manifest main attributes" exception.
        if (name.endsWith(".RSA") || name.endsWith(".DSA") || name.endsWith(".SF"))
          MergeStrategy.discard
        else
          MergeStrategy.first
      }
      case _ => MergeStrategy.first
    }
  case _ => MergeStrategy.first
}

fork in Test := false
parallelExecution in Test := false

initialCommands in console :=
  """
  """
