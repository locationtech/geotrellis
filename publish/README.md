## Publishing GeoTrellis Releases

This directory contains the elements needed to publish GeoTrellis to Sonatype.
It allows to create an _isolated_ environment to publish GeoTrellis, 
since we had lot's of issues with conflicting sbt versions / gpg version / etc. 

## Setup

First, copy the example `Makefile` and `global.sbt` templates:
```
cp Makefile.template Makefile
cp global.sbt.template global.sbt
```

You'll need to have the proper Sonatype credentials in `./sonatype.sbt`:

```scala
realm=Sonatype Nexus Repository Manager
host=oss.sonatype.org
user=username
password=password
```

Then, make sure you update `CREDENTIALS_PATH` in the Makefile to point to this directory.

Add the proper PGP public and private key to `~/.gnupg` (or to any other directory, see `Makefile`). If you need these keys, contact a GeoTrellis maintainer.

Edit the passphrase for the private key (for jar signing) in `./global.sbt`. If you need the passphrase, contact a GeoTrellis maintainer:

```scala
pgpPassphrase := Some(Array('p', 'a', 's', 's', 'w', 'o', 'r', 'd'))
```

Ensure that `./gpg.sbt` that looks like:

```scala
addSbtPlugin("com.jsuereth" % "sbt-pgp" % "1.1.1")
```

As we change SBT versions, we'll need to modify the `Makefile` to point the correct
file locations. A current problem with this process is that you may be prompted for and
need to enter the pgp password manually.

We also have to change the branch that is checked out in the `Dockerfile` to the
proper version branch as we change GeoTrellis versions.

Note: Sonatype requires JARs be signd via PGP, so if you do not have keys
with a distributed public key, you'll have to work through the instructions here:
http://central.sonatype.org/pages/working-with-pgp-signatures.html

## Makefile variables

In the Makefile you can specify the following variables:

```makefile
# docker image default name
IMG                       := geotrellis/publish-geotrellis-container
# docker image default tag
TAG                       := latest
# GeoTrellis release tag
RELEASE_TAG               := vX.Y.Z
# GeoTrellis version suffix that determines the release type
GEOTRELLIS_VERSION_SUFFIX := ""
# path to PGP keys
PGPKEYS_PATH              := ~/.gnupg
# path to Sonatype credentials
CREDENTIALS_PATH          := ~/.ivy2
```

## Building the container

First ensure that `RELEASE_TAG` in `Makefile` is set to the tag you wish to publish.

Then build the container using `make build`. The image will always be rebuilt from
scratch, cloning geotrellis and checking out the version branch.


## Publishing

Run `make publish`. This will run the `publish-to-sonatype.sh` script inside the
container, and push everything to the sonatype staging repository.

After that, you'll have to log in with the appropriate authentication to
https://oss.sonatype.org/, and from there follow the release instructions
at http://central.sonatype.org/pages/releasing-the-deployment.html.
