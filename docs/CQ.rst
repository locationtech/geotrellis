Create a Contribution Questionnaire
===================================

Any dependency changes made to GeoTrellis should be submited as a new CQ into the
`ECLIPSE IP Zilla <https://dev.eclipse.org/ipzilla/query.cgi>`__.

Submitting a CQ
---------------

Visit `https://projects.eclipse.org/projects/locationtech.geotrellis/ <https://projects.eclipse.org/projects/locationtech.geotrellis/>`__
and log into the system.

.. figure:: img/locationtech-geotrellis.png
   :alt: The GeoTrellis project page

On the right, in the ``COMMITTER TOOLS`` side bar, you can find a link `Create a Contribution Questionnaire <https://projects.eclipse.org/projects/locationtech.geotrellis/cq/create>`__

.. figure:: img/geotrellis-cq.png
   :alt: Create a Contribution Questionnaire page

Next, we need to create a Third-Party Code Quest. For this example, we'll be creating a CQ for ``pureconfig v0.10.2``:

.. figure:: img/pb-cq.png
   :alt: A "piggyback" CQ

When typing in the name/version of the dependency, it may sometimes already appear.
If that's the case, then that means someone else has already sent this dependency to Eclipse's IP team.
In that case, the CR could be instantly approved if the IP team has already approved of the given dependency.

Let's create a CQ for ``pureconfig v0.11.0``. There is no such a library, so we'll have to fill the page manually:

.. figure:: img/npb-cq-intro.png
   :alt: A new CQ

The next step would be to full fill the information about it:

.. figure:: img/npb-cq.png
   :alt: A new CQ Step 1

If the library was already submitted but a new version is being requested, one can look at what was already submitted for that library to use as an example.
In our case we can use a `Pureconfig Version 0.10.2 <https://dev.eclipse.org/ipzilla/show_bug.cgi?id=19572>`__

If it is a completely new dependency and it was never validated by the IP team - then it is still required to fill all the fields,
but without any help.

Next, you'll need to go the CQ page and submit any source code released to this CQ (``pureconfig v0.10.2`` for this example):

.. figure:: img/ipzilla-cq.png
   :alt: Add sources to the CQ

One can find source code for their target library in a number of different locations.
The first and probably best place to check is `Maven Central search <https://search.maven.org/search?q=a:pureconfig_2.11>`__:

.. figure:: img/cq-sources-mavencentral.png
   :alt: Download sources

You may upload multiple sources, in case they are all in the same repository.
The example of such a CQ is `AWS SDK CQ <https://dev.eclipse.org/ipzilla/show_bug.cgi?id=19560>`__.

Affter you uploaded all ``sources``, the next step would be to submit a PMC approval request.
To do it, you need to subscribe for the `Technology PMC mailing list <https://dev.locationtech.org/mailman/listinfo/technology-pmc>`__ and to send a request.
The example of such a request can be found in the `Technology PMC mailing list archive <https://dev.locationtech.org/mhonarc/lists/technology-pmc/msg01954.html>`__.
If things are not moving, you can try to leave your request in a `Locationtech Gitter lobby channel <https://gitter.im/locationtech/discuss>`__.
