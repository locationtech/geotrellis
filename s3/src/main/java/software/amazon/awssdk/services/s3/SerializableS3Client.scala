package software.amazon.awssdk.services.s3

import software.amazon.awssdk.regions.Region

import java.net.URI

object SerializableS3Client {
  def default() =
    new DefaultS3ClientBuilder().build()

  def test() =
    new DefaultS3ClientBuilder()
      .endpointOverride(new URI("http://localhost:4572"))
      .region(Region.US_EAST_1)
      .build()
}
