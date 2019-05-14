package geotrellis.spark.io.s3

import software.amazon.awssdk.services.s3.S3Client

/**
  *  Singleton which can be used to customize the default S3 client used throughout geotrellis
  *
  *  Various classes are loaded based on URI patterns and because SPI requires
  *   parameterless constructors, it is necessary to register any customizations
  *   to the S3Client that should be applied by default here.
  *
  */
object S3ClientProducer {
  @transient
  private lazy val client = S3Client.create()

  private var summonClient: () => S3Client =
    () => client

  /**
    * Set an alternative default function for summoning S3Clients
    */
  def set(getClient: () => S3Client): Unit =
    summonClient = getClient

  /**
    * Get the current function registered as default for summong S3Clients
    */
  def get: () => S3Client =
    summonClient
}
