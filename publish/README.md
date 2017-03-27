## Publishing GeoTrellis Releases

This directory contains the elements needed to publish GeoTrellis to Sonatype.

## Setup

You'll need to have the proper Sonatype credentials in `~/.ivy2/.credentials`,
the proper PGP public and private key in `~/.gnupg`,
the password for the private key (for jar signing) in `~/.sbt/0.13/local.sbt`,
and a `~/.sbt/0.13/plugins/plugins.sbt` that looks like:

```
addSbtPlugin("com.jsuereth" % "sbt-pgp" % "1.0.0")
```

As we change SBT versions, we'll need to modify the `Makefile` to point the correct
file locations. A current problem with this process is that you may be prompted for and
need to enter the pgp password manually.

We also have to change the branch that is checked out in the `Dockerfile` to the
proper version branch as we change GeoTrellis versions.

Note: Sonatype requires JARs be signd via PGP, so if you do not have keys
with a distributed public key, you'll have to work through the instructions here:
http://central.sonatype.org/pages/working-with-pgp-signatures.html

## Building the container

First build the container using `make build`. The image will always be rebuilt from
scratch, cloning geotrellis and checking out the version branch.


## Publishing

Run `make publish`. This will run the `publish-to-sonatype.sh` script inside the
container, and push everything to the sonatype staging repository.

After that, you'll have to log in with the appropriate authentication to
https://oss.sonatype.org/, and from there follow the release instructions
at http://central.sonatype.org/pages/releasing-the-deployment.html.
