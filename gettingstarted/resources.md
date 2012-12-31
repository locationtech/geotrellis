---
layout: gettingstarted
title: Geotrellis Resources

tutorial: gettingstarted
num: 5
---

#### Mailing list and IRC
You can find us on IRC at #geotrellis on freenode, or join the [geotrellis-user mailing list](https://groups.google.com/group/geotrellis-user).  We're always interested in what you're working on, if we can help, and any feedback you might have.

#### Scaladocs

If you want to dive directly into the code, you can find *Scaladocs*, the Scala API documentation, for the latest version of the project [here]($siteBaseUrl$/latest/api/index.html#geotrellis.package).  You can also track the current development of GeoTrellis [at our github repository](http://github.com/azavea/geotrellis).

#### Template Project

We have provided a sample project that provides a template for creating a 
geoprocessing web service with GeoTrellis. It is a blank slate for your own
development that provides a development environment that is set up with the
necessary dependencies in place, making it a little easier to get started.

The project loads GeoTrellis as a library, includes some basic configuration,
and has a very simple "hello world" web service in place for you to edit.
The template contains [step-by-step instructions](https://github.com/azavea/geotrellis.g8).

If you are a first time GeoTrellis user, you can use this template with the
tutorial included in this documentation to start exploring how to build your
own geoprocessing service.

#### Demonstration project

The /demo directory in the github repository includes code from the tutorial (in this guide) and a
demonstration project that provides a REST service that performs a geoprocessing operation and returns a
PNG image to the user.
