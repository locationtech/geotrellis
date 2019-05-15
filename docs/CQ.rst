Create a Contribution Questionnaire
===================================

All the new dependencies and all the dependencies updates should be submited as a new CQ into the
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

Next, we need to create a Third-Party Code Request usually. All the next steps would be based on a particular library example.
Let's create a CQ for ``pureconfig v0.10.2``:

.. figure:: img/pb-cq.png
   :alt: A "piggyback" CQ

If your library of your version is here, then select it. It is the easiest result and it means that somebody have already sent a request to
the ``Eclipse IP`` team and the dependency (probably) had been validated. It means that this dep can be approved automatically.

Let's create a CQ for ``pureconfig v0.11.0``. There is no such a library, so we'll have to fill the page manually:

.. figure:: img/npb-cq-intro.png
   :alt: A new CQ

The next step would be to full fill the information about it:

.. figure:: img/npb-cq.png
   :alt: A new CQ Step 1

The question is - how to do it? If this library but of a previous version was already submitted, we can use it as an example to fill
the page. In our case we can use `Pureconfig Version 0.10.2 <https://dev.eclipse.org/ipzilla/show_bug.cgi?id=19572>`__

Go into the CQ page and submit all the ``sources`` related to this CQ (for this particular example I used ``pureconfig v0.10.2``):

.. figure:: img/ipzilla-cq.png
   :alt: Add sources to the CQ

How to get sources? For these purposes it is possible to use `Maven Central search <https://search.maven.org/search?q=a:pureconfig_2.11>`__:

.. figure:: img/cq-sources-mavencentral.png
   :alt: Download sources

You may upload multiple sources, in case they are all in the same repository.
The example of such a CQ is `AWS SDK CQ <https://dev.eclipse.org/ipzilla/show_bug.cgi?id=19560>`__.

After you uploaded all ``sources``, the next step would be to submit a PMC approval request. To do it, you need to subscribe for the
`Technology PMC mailing list <https://dev.locationtech.org/mailman/listinfo/technology-pmc>`__ and to send a request here.
The example of such a request can be found in the `Technology PMC mailing list archive <https://dev.locationtech.org/mhonarc/lists/technology-pmc/msg01954.html>`__.
If things are not moving, you can try to leave your request in a `Locationtech Gitter lobby channel <https://gitter.im/locationtech/discuss>`__
