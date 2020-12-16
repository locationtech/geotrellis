module.exports = {
  title: 'GeoTrellis',
  tagline: 'A geographic data processing engine for high performance applications',
  url: 'https://geotrellis.io',
  baseUrl: '/',
  onBrokenLinks: 'throw',
  onBrokenMarkdownLinks: 'warn',
  favicon: 'img/favicon-32x32.png',
  organizationName: 'locationtech', // Usually your GitHub org/user name.
  projectName: 'geotrellis', // Usually your repo name.
  themeConfig: {
    prism: {
      additionalLanguages: ['java', 'scala'],
    },
    navbar: {
      title: 'GeoTrellis',
      logo: {
        alt: 'GeoTrellis Logo',
        src: 'img/logo.svg',
      },
      items: [
        {
          to: 'docs/',
          activeBasePath: 'docs',
          label: 'Documentation',
          position: 'right',
        },
        {
          href: 'https://github.com/locationtech/geotrellis',
          label: 'GitHub',
          position: 'right',
        },
      ],
    },
    footer: {
      style: 'dark',
      links: [
        {
          title: 'Docs',
          items: [
            {
              label: 'Getting Started',
              to: 'docs/',
            },
            {
              label: 'ScalaDocs',
              href: 'https://geotrellis.github.io/scaladocs/latest/index.html',
            }
          ],
        },
        {
          title: 'Community',
          items: [
            {
              label: 'Gitter.im',
              href: 'https://gitter.im/geotrellis/geotrellis',
            },
            {
              label: 'GitHub Issues',
              href: 'https://github.com/locationtech/geotrellis/issues',
            },
          ],
        },
        {
          title: 'More',
          items: [
            {
              label: 'Azavea Blog',
              href: 'https://www.azavea.com/blog/'
            },
            {
              label: 'GitHub',
              href: 'https://github.com/locationtech/geotrellis',
            },
          ],
        },
      ],
      copyright: `Copyright © 2016 - ${new Date().getFullYear()} Azavea, Inc.`,
    },
  },
  presets: [
    [
      '@docusaurus/preset-classic',
      {
        docs: {
          sidebarPath: require.resolve('./sidebars.js'),
          // Please change this to your repo.
          editUrl:
            'https://github.com/locationtech/geotrellis/edit/master/website/',
        },
        theme: {
          customCss: require.resolve('./src/css/custom.css'),
        },
      },
    ],
  ],
};
