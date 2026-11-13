require("dotenv").config();
const isSaas = process.env.DOCUSAURUS_IS_SAAS === "true";

// Archived documentation for legacy versions is hosted separately at
// archive.docs.orrery.dev (see the version dropdown below). Cross-site links
// open in a new tab; within-site version switching stays in the same tab.
const ARCHIVE_BASE = "https://archive.docs.orrery.dev/docs";
const EXTERNAL_LINK_ICON =
  '<svg width="12" height="12" aria-hidden="true" viewBox="0 0 24 24"><path fill="currentColor" d="M21 13v10h-21v-19h12v2h-10v15h17v-8h2zm3-12h-10.988l4.035 4-6.977 7.07 2.828 2.828 6.977-7.07 4.125 4.172v-11z"></path></svg>';
// [version label, path under /docs/<version>/]. Every version links to its
// /features page EXCEPT 0.13.1, which has no features page in the archive and
// so points to its version home instead.
const ARCHIVED_VERSIONS = [
  ["1.7.0", "1.7.0/features"],
  ["1.6.0", "1.6.0/features"],
  ["1.5.0", "1.5.0/features"],
  ["1.3.0", "1.3.0/features"],
  ["1.1.0", "1.1.0/features"],
  ["1.0.0", "1.0.0/features"],
  ["0.15.0", "0.15.0/features"],
  ["0.14.1", "0.14.1/features"],
  ["0.14.0", "0.14.0/features"],
  ["0.13.1", "0.13.1/"],
  ["0.13.0", "0.13.0/features"],
  ["0.12.1", "0.12.1/features"],
  ["0.12.0", "0.12.0/features"],
  ["0.11.0", "0.11.0/features"],
  ["0.10.5", "0.10.5/features"],
];

module.exports = {
  title: process.env.DOCUSAURUS_CONFIG_TITLE || "Orrery",
  tagline: "The #1 Open Source Metadata Platform",
  url: process.env.DOCUSAURUS_CONFIG_URL || "https://docs.orrery.dev",
  baseUrl: process.env.DOCUSAURUS_CONFIG_BASE_URL || "/",
  onBrokenLinks: "throw",
  onBrokenMarkdownLinks: "throw",
  favicon: "img/favicon.ico",
  organizationName: "Dxlxz", // Usually your GitHub org/user name.
  projectName: "orrery", // Usually your repo name.
  staticDirectories: ["static"],
  stylesheets: ["https://fonts.googleapis.com/css2?family=Manrope:wght@400;500;700&display=swap"],
  headTags: [
    {
      tagName: 'meta',
      attributes: {
        httpEquiv: 'Content-Security-Policy',
        content: "frame-ancestors 'self' https://*.orrery.dev https://orrery.dev http://localhost:*"
      }
    },
  ],
  scripts: [
    {
      src: "https://tools.luckyorange.com/core/lo.js?site-id=28ea8a38",
      async: true,
      defer: true,
    },
    {
      src: "/scripts/rb2b.js",
      async: true,
      defer: true,
    },
    {
      src: "https://app.revenuehero.io/scheduler.min.js"
    },
    {
      src: "https://tag.clearbitscripts.com/v1/pk_2e321cabe30432a5c44c0424781aa35f/tags.js",
      referrerPolicy: "strict-origin-when-cross-origin"
    },
    {
      src: "/scripts/reo.js",
    },
    {
      id: "runllm-widget-script",
      type: "module",
      src: "https://widget.runllm.com",
      crossorigin: "true",
      "runllm-name": "Orrery",
      "runllm-assistant-id": "81",
      "runllm-position": "BOTTOM_RIGHT",
      "runllm-keyboard-shortcut": "Mod+j",
      "runllm-preset": "docusaurus",
      "runllm-theme-color": "#1890FF",
      "runllm-brand-logo": "https://docs.orrery.dev/img/orrery-logo-color-mark.svg",
      "runllm-community-url": "https://orrery.dev/slack",
      "runllm-community-type": "slack",
      "runllm-disable-ask-a-person": "true",
      async: true,
    },
  ],
  noIndex: isSaas,
  customFields: {
    isSaas: isSaas,
  },

  // See https://github.com/facebook/docusaurus/issues/4765
  // and https://github.com/langchain-ai/langchainjs/pull/1568
  webpack: {
    jsLoader: (isServer) => ({
      loader: require.resolve("swc-loader"),
      options: {
        jsc: {
          parser: {
            syntax: "typescript",
            tsx: true,
          },
          target: "es2017",
        },
        module: {
          type: isServer ? "commonjs" : "es6",
        },
      },
    }),
  },

  themeConfig: {
    // ...(!isSaas && {
    //   announcementBar: {
    //     id: "announcement",
    //     content:
    //       '<div><img src="/img/orrery-logo-white-mark.svg" /><p><strong>Orrery Cloud</strong><span> &nbsp;Orrery delivers an easy to consume Orrery platform for the enterprise</span></p></div> <a href="https://www.orrery.dev/demo?utm_source=orrery&utm_medium=referral&utm_campaign=orrery_signup" target="_blank" class="button button--primary">Sign Up for Orrery Cloud&nbsp;→</a>',
    //     backgroundColor: "#070707",
    //     textColor: "#ffffff",
    //     isCloseable: false,
    //   },
    // }),
    announcementBar: {
      id: "announcement-4",
      content:
        '<div class="shimmer-banner"><span>CONTEXT 2026: Join the virtual summit on context management, November 4</span><a href="https://orrery.dev/context/" target="_blank" class="button"><div>Save my spot<span> →</span></div></a></div>',
      backgroundColor: "transparent",
      textColor: "#ffffff",
      isCloseable: false,
    },

    colorMode: {
      // Only support light mode.
      defaultMode: 'light',
      disableSwitch: true,
      respectPrefersColorScheme: false,
    },
    navbar: {
      title: null,
      logo: {
        alt: "Orrery Logo",
        src: `img/${isSaas ? "orrery" : "orrery"}-logo-color-light-horizontal.svg`,
        srcDark: `img/${isSaas ? "orrery" : "orrery"}-logo-color-dark-horizontal.svg`,
        href: "https://orrery.dev",
      },
      items: [
        {
          to: "/docs",
          activeBasePath: "docs",
          label: "Docs",
          position: "right",
        },
        {
          to: "/integrations",
          activeBasePath: "integrations",
          label: "Integrations",
          position: "right",
        },
        {
          type: "docsVersionDropdown",
          position: "left",
          dropdownActiveClassDisabled: true,
          dropdownItemsAfter: [
            {
              type: "html",
              value: '<hr class="dropdown-separator" style="margin: 0.4rem;">',
            },
            {
              type: "html",
              value: '<div class="dropdown__link"><b>Archived versions</b></div>',
            },
            // Cross-site links to the standalone archive open in a new tab.
            ...ARCHIVED_VERSIONS.map(([label, path]) => ({
              type: "html",
              value: `<a class="dropdown__link" target="_blank" rel="noopener noreferrer" href="${ARCHIVE_BASE}/${path}">${label}${EXTERNAL_LINK_ICON}</a>`,
            })),
          ],
        },
        {
          href: "https://orrery.dev/slack",
          html: `
            <style>
              .slack-logo:hover {
                opacity: 0.8;
              }
            </style>
            <img class='slack-logo' src='https://upload.wikimedia.org/wikipedia/commons/d/d5/Slack_icon_2019.svg', alt='slack', height='20px' style='margin: 10px 0 0 0;'/>
          `,
          position: "right",
        },
        {
          href: "https://github.com/Dxlxz/orrery",
          html: `
            <style>
              .github-logo:hover {
                opacity: 0.8;
              }
            </style>
            <img class='github-logo' src='https://upload.wikimedia.org/wikipedia/commons/9/91/Octicons-mark-github.svg', alt='slack', height='20px' style='margin: 10px 0 0 0;'/>
          `,
          position: "right",
        },
      ],
    },
    footer: {
      style: "dark",
      links: [
        {
          title: "Docs",
          items: [
            {
              label: "Introduction",
              to: "docs/",
            },
            {
              label: "Quickstart",
              to: "docs/quickstart",
            },
          ],
        },
        {
          title: "Community",
          items: [
            {
              label: "Slack",
              href: "https://orrery.dev/slack",
            },
            {
              label: "YouTube",
              href: "https://www.youtube.com/channel/UC3qFQC5IiwR5fvWEqi_tJ5w",
            },
            {
              label: "Blog",
              href: "https://orrery.dev/blog/",
            },
            {
              label: "Town Halls",
              to: "docs/townhalls",
            },
            {
              label: "Customer Stories",
              href: "https://orrery.dev/resources/?2004611554=dh-stories",
            },
          ],
        },
        {
          title: "More",
          items: [
            {
              label: "Demo",
              to: "https://demo.orrery.dev/",
            },
            {
              label: "Product Tours",
              href: "https://tours.orrery.dev/?utm_source=oss-docs&utm_medium=oss-referral&utm_campaign=oss-product-tours&utm_content=docs-footer",
            },
            {
              label: "Free Trial",
              href: "https://orrery.dev/free-trial/",
            },
            {
              label: "Roadmap",
              href: "https://feature-requests.orrery.dev/roadmap",
            },
            {
              label: "Contributing",
              to: "docs/contributing",
            },
            {
              label: "GitHub",
              href: "https://github.com/Dxlxz/orrery",
            },
            {
              label: "Feature Requests",
              href: "https://feature-requests.orrery.dev/",
            },
          ],
        },
      ],
      copyright: `Copyright © 2015-${new Date().getFullYear()} Orrery Project Authors.`,
    },
    prism: {
      // https://docusaurus.io/docs/markdown-features/code-blocks#theming
      // theme: require("prism-react-renderer/themes/vsLight"),
      // darkTheme: require("prism-react-renderer/themes/vsDark"),
      additionalLanguages: ["ini", "java", "graphql", "shell-session"],
    },
    algolia: {
      // This is the "Search API Key" in Algolia, which means that it is ok to be public.
      apiKey: "2adf840a044a5ecbf7bdaac88cbf9ee5",
      appId: "RK0UG797F3",
      indexName: "orreryproject",
      insights: true,
      contextualSearch: true,
      // debug: true,
    },
    tableOfContents: {
      minHeadingLevel: 2,
      maxHeadingLevel: 4,
    },
  },
  presets: [
    [
      "@docusaurus/preset-classic",
      {
        docs: {
          lastVersion: "current",
          versions: {
            current: {
              label: "1.8.0",
              banner: 'none',
            },
          },
          path: "genDocs",
          sidebarPath: require.resolve("./sidebars.js"),
          ...(!isSaas && {
            editUrl: ({ versionDocsDirPath, docPath }) => {
              // Hide the edit button for docs generated by plugins (Sphinx, GraphQL)
              if (docPath.startsWith('python-sdk/') || docPath.startsWith('graphql/')) {
                return undefined;
              }
              // Note: This fallback URL relies on generateDocsDir.ts pre-injecting `custom_edit_url` 
              // for manual docs. If that script's behavior changes, this mapping to 'genDocs' 
              // could result in 404s, as genDocs is git-ignored.
              return `https://github.com/Dxlxz/orrery/blob/master/${versionDocsDirPath}/${docPath}`;
            },
          }),
          numberPrefixParser: false,
          // TODO: make these work correctly with the doc generation
          showLastUpdateAuthor: false,
          showLastUpdateTime: false,
          // Exclude internal files from public docs
          // Note: We must include Docusaurus defaults since providing exclude overrides them
          exclude: [
            // Docusaurus defaults
            '**/_*.{js,jsx,ts,tsx,md,mdx}',
            '**/_*/**',
            '**/*.test.{js,jsx,ts,tsx}',
            '**/__tests__/**',
            // Agent context files — not public docs (see generateDocsDir.ts filter_patterns)
            '**/CLAUDE.md',
            '**/CLAUDE.MD',
            '**/AGENTS.md',
          ],
        },
        blog: {
          blogTitle: "Orrery Learn",
          blogSidebarTitle: "Orrery Learn",
          blogDescription: "Learn about the hot topics in the data ecosystem and how Orrery can help you with your data journey.",
          path: "src/learn",
          routeBasePath: "learn",
          postsPerPage: "ALL",
          blogListComponent: "../src/learn/_components/LearnListPage",
        },
        theme: {
          customCss: [
            isSaas ? require.resolve("./src/styles/orrery.scss") : require.resolve("./src/styles/orrery.scss"),
            require.resolve("./src/styles/global.scss"),
            require.resolve("./src/styles/sphinx.scss"),
            require.resolve("./src/styles/config-table.scss"),
          ],
        },
        pages: {
          path: "src/pages",
          mdxPageComponent: "@theme/MDXPage",
        },
        googleTagManager: {
          containerId: 'GTM-5M8T9HNN',
        },
        gtag: {
          trackingID: "G-H5QDNJNMHY",
        },
      },
    ],
  ],
  plugins: [
    [
      '@docusaurus/plugin-client-redirects',
      {
        createRedirects(existingPath) {
          if (existingPath.includes('/docs')) {
            return [
              existingPath.replace('/docs', '/docs/next'),
              existingPath.replace('/docs', '/docs/0.13.0'),
              existingPath.replace('/docs', '/docs/0.12.1'),
              existingPath.replace('/docs', '/docs/0.11.0'),
              existingPath.replace('/docs', '/docs/0.10.5'),
            ];
          }
          return undefined; // Return a falsy value: no redirect created
        },
        redirects: [
          {
            from: '/docs/how/updating-orrery',
            to: '/docs/how/breaking-changes',
          },
          {
            from: '/docs/managed-orrery/operator-guide/setting-up-remote-ingestion-executor',
            to: '/docs/managed-orrery/remote-executor/about',
          },
          {
            from: '/docs/managed-orrery/observe/smart-assertions',
            to: '/docs/managed-orrery/observe/anomaly-detection',
          },
          {
            from: '/docs/features/feature-guides/observe',
            to: '/docs/managed-orrery/observe/overview',
          },
          {
            from: '/docs/managed-orrery/context/overview',
            to: '/docs/managed-orrery/build-a-data-agent/overview',
          },
          {
            from: '/docs/managed-orrery/context/configure-context-generation',
            to: '/docs/features/feature-guides/context/context-generation',
          },
          {
            from: '/docs/managed-orrery/context/review-context-proposals',
            to: '/docs/managed-orrery/build-a-data-agent/generate-context',
          },
          {
            from: '/docs/managed-orrery/context/activate-context',
            to: '/docs/managed-orrery/build-a-data-agent/activate-context',
          },
          {
            // The root README is no longer built into the docs site.
            from: [
              '/docs/introduction',
              '/docs/next/introduction',
              '/docs/0.13.0/introduction',
              '/docs/0.12.1/introduction',
              '/docs/0.11.0/introduction',
              '/docs/0.10.5/introduction',
            ],
            to: '/docs/features',
          },
        ],
      },
    ],
    // Hotfix release notes live as appended sections in the base release page
    // (not standalone .md files). We can't use @docusaurus/plugin-client-redirects
    // because its PathnameSchema rejects hash fragments in `to`. Instead, write
    // meta-refresh stub files directly in postBuild — same mechanism the redirect
    // plugin uses internally, but with anchor support.
    function hotfixReleaseNoteRedirects() {
      const RELEASE_NOTES_BASE = '/docs/managed-orrery/release-notes';
      const HOTFIX_REDIRECTS = [
        ['v_1_1_1',    'v_1_1_0',  'v111'],
        ['v_1_1_2',    'v_1_1_0',  'v112'],
        ['v_1_1_3',    'v_1_1_0',  'v113'],
        ['v_1_1_4',    'v_1_1_0',  'v114'],
        ['v_1_0_1',    'v_1_0_0',  'v101'],
        ['v_1_0_2',    'v_1_0_0',  'v102'],
        ['v_1_0_3',    'v_1_0_0',  'v103'],
        ['v_0_3_17_1', 'v_0_3_17', 'v03171'],
        ['v_0_3_17_2', 'v_0_3_17', 'v03172'],
        ['v_0_3_17_3', 'v_0_3_17', 'v03173'],
        ['v_0_3_17_4', 'v_0_3_17', 'v03174'],
        ['v_0_3_17_5', 'v_0_3_17', 'v03175'],
        ['v_0_3_16_1', 'v_0_3_16', 'v03161-orrery'],
        ['v_0_3_16_2', 'v_0_3_16', 'v03162-orrery'],
        ['v_0_3_16_3', 'v_0_3_16', 'v03163-orrery'],
        ['v_0_3_16_4', 'v_0_3_16', 'v03164-orrery'],
        ['v_0_3_16_5', 'v_0_3_16', 'v03165-orrery'],
        ['v_0_3_16_6', 'v_0_3_16', 'v03166-orrery'],
        ['v_0_3_16_7', 'v_0_3_16', 'v03167-orrery'],
        ['v_0_3_15_4', 'v_0_3_15', 'v03154-orrery'],
        ['v_0_3_15_5', 'v_0_3_15', 'v03155-orrery'],
        ['v_0_3_15_6', 'v_0_3_15', 'v03156-orrery'],
        ['v_0_3_14_1', 'v_0_3_14', 'v03141-orrery'],
        ['v_0_3_13_1', 'v_0_3_13', 'v03131-orrery'],
        ['v_0_3_13_2', 'v_0_3_13', 'v03132-orrery'],
        ['v_0_3_13_3', 'v_0_3_13', 'v03133-orrery'],
        ['v_0_3_12_1', 'v_0_3_12', 'v03121'],
        ['v_0_3_12_2', 'v_0_3_12', 'v03122'],
        ['v_0_3_12_3', 'v_0_3_12', 'v03123'],
        ['v_0_3_12_4', 'v_0_3_12', 'v03124'],
        ['v_0_3_11_1', 'v_0_3_11', 'v03111'],
        ['v_0_3_10_1', 'v_0_3_10', 'v03101'],
        ['v_0_3_10_2', 'v_0_3_10', 'v03102'],
        ['v_0_3_10_3', 'v_0_3_10', 'v03103'],
        ['v_0_3_10_4', 'v_0_3_10', 'v03104'],
        ['v_0_3_9_2',  'v_0_3_9',  'v0392'],
        ['v_0_3_8_2',  'v_0_3_8',  'v0382'],
        ['v_0_3_7_3',  'v_0_3_7',  'v0373'],
        ['v_0_3_7_4',  'v_0_3_7',  'v0374'],
        ['v_0_3_7_5',  'v_0_3_7',  'v0375'],
        ['v_0_3_7_6',  'v_0_3_7',  'v0376'],
        ['v_0_3_7_7',  'v_0_3_7',  'v0377'],
        ['v_0_3_7_8',  'v_0_3_7',  'v0378'],
      ];
      return {
        name: 'hotfix-release-note-redirects',
        async postBuild({ outDir, baseUrl }) {
          const fs = require('fs-extra');
          const path = require('path');
          const joinUrl = (...parts) =>
            ('/' + parts.join('/')).replace(/\/+/g, '/');
          for (const [fromSlug, toSlug, anchor] of HOTFIX_REDIRECTS) {
            const fromPath = joinUrl(baseUrl, RELEASE_NOTES_BASE, fromSlug);
            const toPath = joinUrl(baseUrl, RELEASE_NOTES_BASE, toSlug) + '/';
            const toUrl = toPath + '#' + anchor;
            const filePath = path.join(outDir, fromPath, 'index.html');
            const html = `<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="utf-8">
<title>Redirecting…</title>
<link rel="canonical" href="${toUrl}">
<meta name="robots" content="noindex">
<meta http-equiv="refresh" content="0; url=${toUrl}">
</head>
<body>
<p><a href="${toUrl}">Click here if you are not redirected.</a></p>
<script>window.location.replace(${JSON.stringify(toPath)} + window.location.search + ${JSON.stringify('#' + anchor)});</script>
</body>
</html>
`;
            await fs.outputFile(filePath, html);
          }
        },
      };
    },
    ["@docusaurus/plugin-ideal-image", { quality: 100, sizes: [320, 640, 1280, 1440, 1600] }],
    "docusaurus-plugin-sass",
    [
      "docusaurus-graphql-plugin",
      {
        schema: "./graphql/combined.graphql",
        routeBasePath: "/docs/graphql",
      },
    ],
    // [
    //   require.resolve("@easyops-cn/docusaurus-search-local"),
    //   {
    //     // `hashed` is recommended as long-term-cache of index file is possible.
    //     hashed: true,
    //     language: ["en"],
    //     docsDir: "genDocs",
    //     blogDir: [],
    //   },
    // ],
  ],
};
