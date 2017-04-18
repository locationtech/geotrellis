# Contributing

We value all kinds of contributions from the community, not just actual
code. Perhaps the easiest and yet one of the most valuable ways of helping
us improve GeoTrellis is to ask questions, voice concerns or propose
improvements on the [Mailing List](https://groups.google.com/group/geotrellis-user).

If you do like to contribute actual code in the form of bug fixes, new
features or other patches this page gives you more info on how to do it.


## Building GeoTrellis

1. Install SBT (the master branch is currently built with SBT 0.13.12).
2. Check out this repository.
3. Pick the branch corresponding to the version you are targeting
4. Run `sbt test` to compile the suite and run all tests.

## Style Guide

We try to follow the [Scala Style Guide]((http://docs.scala-lang.org/style/))
as closely as possible, although you will see some variations throughout the
codebase. When in doubt, follow that guide.

## Git Branching Model

The GeoTrellis team follows the standard practice of using the `master`
branch as main integration branch.

## Git Commit Messages

We follow the 'imperative present tense' style for commit messages. (e.g.
"Add new EnterpriseWidgetLoader instance")

## Issue Tracking

If you find a bug and would like to report it please go there and create
an issue. As always, if you need some help join us on
[Gitter](https://gitter.im/locationtech/geotrellis) to chat with a
developer.

## Pull Requests

If you'd like to submit a code contribution please fork GeoTrellis and send
us pull request against the `master` branch. Like any other open source
project, we might ask you to go through some iterations of discussion and
refinement before merging.

> NOTE: Code contributions cannot be accepted until we've received
> your signed CLAs (contributor license agreement). See the bottom of
> this document for more information.

## Contributing documentation

GeoTrellis documentation comes in two flavors: markdown and scaladoc. The
[latest scaladocs](https://geotrellis.github.io/scaladocs/latest/#geotrellis.package)
are generated upon Travis' successful compilation of the master branch.
[GeoTrellis markdown documentation](http://geotrellis.readthedocs.io/en/latest/)
is version controlled with the rest of the source.

When adding or editing documentation, keep in mind the following file structure:

- `docs/tutorials/` contains simple beginner tutorials with concrete goals
- `docs/guide/` contains detailed explanations of GeoTrellis concepts
- `docs/architecture` contains in-depth discussion on GeoTrellis implementation details

To visually debug your changes in real time, consider using the [mkdocs
tool](http://www.mkdocs.org/). It should be available in your package
repositories for Linux and Mac users.

## Contributor License Agreements (CLA)

Contributions to the project, no matter what kind, are always very
welcome.
Everyone who contributes code to GeoTrellis will be asked to sign the Eclipse Contributor Agreement.
You can electronically sign the [Eclipse Contributor Agreement here](https://www.eclipse.org/legal/ECA.php).
