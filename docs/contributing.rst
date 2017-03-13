Contributing
============

We value all kinds of contributions from the community, not just actual
code. Perhaps the easiest and yet one of the most valuable ways of
helping us improve GeoTrellis is to ask questions, voice concerns or
propose improvements on the `Mailing
List <https://locationtech.org/mailman/listinfo/geotrellis-user>`__.

If you do like to contribute actual code in the form of bug fixes, new
features or other patches this page gives you more info on how to do it.

Building GeoTrellis
-------------------

1. Install SBT (the master branch is currently built with SBT 0.13.12).
2. Check out this repository.
3. Pick the branch corresponding to the version you are targeting
4. Run ``sbt test`` to compile the suite and run all tests.

Style Guide
-----------

We try to follow the `Scala Style
Guide <(http://docs.scala-lang.org/style/)>`__ as closely as possible,
although you will see some variations throughout the codebase. When in
doubt, follow that guide.

Git Branching Model
-------------------

The GeoTrellis team follows the standard practice of using the
``master`` branch as main integration branch.

Git Commit Messages
-------------------

We follow the 'imperative present tense' style for commit messages.
(e.g. "Add new EnterpriseWidgetLoader instance")

Issue Tracking
--------------

If you find a bug and would like to report it please go there and create
an issue. As always, if you need some help join us on
`Gitter <https://gitter.im/locationtech/geotrellis>`__ to chat with a
developer.

Pull Requests
-------------

If you'd like to submit a code contribution please fork GeoTrellis and
send us pull request against the ``master`` branch. Like any other open
source project, we might ask you to go through some iterations of
discussion and refinement before merging.

As part of the Eclipse IP Due Diligence process, you'll need to do some
extra work to contribute. This is part of the requirement for Eclipse
Foundation projects (`see this page in the Eclipse
wiki <https://wiki.eclipse.org/Development_Resources/Handling_Git_Contributions#Git>`__
You'll need to sign up for an Eclipse account **with the same email you
commit to github with**. See the ``Eclipse Contributor Agreement`` text
below. Also, you'll need to signoff on your commits, using the
``git commit -s`` flag. See
https://help.github.com/articles/signing-tags-using-gpg/ for more info.

Eclipse Contributor Agreement (ECA)
-----------------------------------

Contributions to the project, no matter what kind, are always very
welcome. Everyone who contributes code to GeoTrellis will be asked to
sign the Eclipse Contributor Agreement. You can electronically sign the
`Eclipse Contributor Agreement
here <https://www.eclipse.org/legal/ECA.php>`__.

Editing these Docs
------------------

Contributions to these docs are welcome as well. To build them on your own
machine, ensure that ``sphinx`` and ``make`` are installed.

Installing Dependencies
^^^^^^^^^^^^^^^^^^^^^^^

Ubuntu 16.04
''''''''''''

.. code:: console

   > sudo apt-get install python-sphinx python-sphinx-rtd-theme

Arch Linux
''''''''''

.. code:: console

   > sudo pacman -S python-sphinx python-sphinx_rtd_theme

MacOS
'''''

``brew`` doesn't supply the sphinx binaries, so use ``pip`` here.

Pip
'''

.. code:: console

   > pip install sphinx sphinx_rtd_theme

Building the Docs
^^^^^^^^^^^^^^^^^

Assuming you've cloned the `GeoTrellis repo
<https://github.com/locationtech/geotrellis>`__, you can now build the docs
yourself. Steps:

1. Navigate to the ``docs/`` directory
2. Run ``make html``
3. View the docs in your browser by opening ``_build/html/index.html``

.. note:: Changes you make will not be automatically applied; you will have
          to rebuild the docs yourself. Luckily the docs build in about a second.

File Structure
^^^^^^^^^^^^^^

When adding or editing documentation, keep in mind the following file
structure:

-  ``docs/tutorials/`` contains simple beginner tutorials with concrete
   goals
-  ``docs/guide/`` contains detailed explanations of GeoTrellis concepts
-  ``docs/architecture`` contains in-depth discussion on GeoTrellis
   implementation details
