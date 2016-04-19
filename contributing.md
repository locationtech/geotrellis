# Contributing

We value all kinds of contributions from the community, not just actual
code. Perhaps the easiest and yet one of the most valuable ways of
helping us improve GeoTrellis is to ask questions, voice concerns or
propose improvements on the :ref:`Mailing List`.

If you do like to contribute actual code in the form of bug fixes, new
features or other patches this page gives you more info on how to do it.


## Building GeoTrellis

1. Install SBT (the master branch is currently built with SBT 0.12.3).
2. Check out this repository.
3. Pick the branch corresponding to the version you are targeting
4. Run `sbt test` to compile the suite and run all tests.

## Style Guide

We try to follow the Scala Style Guide as closely as possible,
although you will see some variations throughout the codebase. When in
doubt, follow that guide.

http://docs.scala-lang.org/style/

## git Branching Model

The GeoTrellis team follows the standard practice of using the
`master` branch as main integration branch.

## git Commit Messages

We follow the 'imperative present tense' style for commit messages.
(e.g. "Add new EnterpriseWidgetLoader instance")

## Issue Tracking

If you find a bug and would like to report it please go there and create
an issue. As always, if you need some help join us on
[Gitter](https://gitter.im/geotrellis/geotrellis) to chat with a
developer.

## Pull Requests

If you'd like to submit a code contribution please fork GeoTrellis
and send us pull request against the `master` branch. Like any other
open source project, we might ask you to go through some iterations
of discussion and refinement before merging.

> NOTE: Code contributions cannot be accepted until we've received
> your signed CLAs (contributor license agreement). See the bottom of
> this document for more information.

## Contributing documentation

GeoTrellis documentation comes in two flavors: markdown and scaladoc.
The [latest
scaladocs](https://geotrellis.github.io/scaladocs/latest/#geotrellis.package)
are generated upon Travis' successful compilation of the master branch.
GeoTrellis markdown documentation is version controlled [with the rest
of the source](./docs).

## Contributor License Agreements (CLA)

Contributions to the project, no matter what kind, are always very
welcome.
Everyone who contributes code to GeoTrellis will be asked to sign the
GeoTrellis and Eclipse Contributor License Agreements. We currently require both CLAs to be signed. The Eclipse CLA requires an Eclipse account and is signed electronically. The GeoTrellis CLA will be kept as a hard copy by Azavea, Inc.

1. Electronically sign the [Eclipse Contributor License
   Agreement](http://www.eclipse.org/legal/CLA.php).

2. Download a copy of the [GeoTrellis Inidividual CLA](http://geotrellis.github.com/files/2014_05_20-GeoTrellis-Open-Source-Contributor-Agreement-Individual.pdf?raw=true), 
   or the [GeoTrellis Corporate CLA](http://geotrellis.github.com/files/2012_04_04-GeoTrellis-Open-Source-Contributor-Agreement-Corporate.pdf?raw=true).

3. Sign the GeoTrellis CLA, either with PDF signing software or by printing out the CLA and hand signing.

4. Send the GeoTrellis CLA to Azavea by one of:
  - Scanning and emailing the document to cla -at- azavea -dot- com
  - Faxing a copy to +1-215-925-2600.
  - Mailing a hardcopy to:
    Azavea, 990 Spring Garden Street, 5th Floor, Philadelphia, PA 19107 USA

