# GeoTrellis Docs Site

The GeoTrellis docs website is built using [Docusaurus 2](https://v2.docusaurus.io/).

## Developing on the docs

Ensure you have Node 12+ installed.

In one terminal, run `./sbt "project mdoc;mdoc --watch"` in the parent directory.
This will run the mdoc watcher which recompiles docs in `../docs-mdoc` and writes the output to `./docs`.

Install project dependencies:

```shell
yarn install --pure-lockfile
```

Start the docusaurus dev server with:

```shell
yarn start
```

This will automatically recompile the docs site whenever the mdoc watcher writes updates to `./docs`.

### Adding a new Doc page

Stop the sbt mdoc watcher and the yarn dev server.

Add a new file to `../docs-mdoc`.

Add an entry to `./sidebars.js` so that the new page appears somewhere in the doc tree. The [Docusaurus sidebar](https://v2.docusaurus.io/docs/docs-introduction#sidebar) docs have more info on how to configure the sidebar.

Restart both the mdoc watcher and the docusaurus server as described above.
