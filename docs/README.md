# OpenToken Documentation

This directory contains the documentation for OpenToken that is published to GitHub Pages.

## Documentation Site

The documentation is available at: [https://mattwise-42.github.io/OpenToken](https://mattwise-42.github.io/OpenToken)

## Contents

- `index.md` - Homepage for the documentation site
- `dev-guide-development.md` - Comprehensive development guide
- `metadata-format.md` - Metadata output format documentation
- `images/` - Images and diagrams used in documentation
- `_config.yml` - Jekyll configuration for GitHub Pages

## Building Locally

To preview the documentation site locally:

```bash
# Install Jekyll and Bundler
gem install bundler jekyll

# Create a Gemfile in the docs directory
cat > Gemfile << EOF
source "https://rubygems.org"
gem "github-pages", group: :jekyll_plugins
gem "webrick"
EOF

# Install dependencies
bundle install

# Serve the site locally
bundle exec jekyll serve

# Open http://localhost:4000/OpenToken in your browser
```

## Automatic Deployment

The documentation is automatically deployed to GitHub Pages when:
- Changes are pushed to the `main` branch in the `docs/` directory, `README.md`, or the workflow file
- The workflow is manually triggered via GitHub Actions

The deployment workflow is defined in `.github/workflows/deploy-docs.yml`.

## Adding New Documentation

1. Create a new Markdown file in the `docs/` directory
2. Add Jekyll front matter at the top:
   ```yaml
   ---
   layout: default
   title: Your Page Title
   ---
   ```
3. Write your content in Markdown
4. Optionally add the page to the navigation in `_config.yml`
5. Commit and push to trigger automatic deployment

## Theme

The documentation uses the [Cayman](https://github.com/pages-themes/cayman) theme, which provides a clean, professional look suitable for technical documentation.

## Enabling GitHub Pages

To enable GitHub Pages for this repository:

1. Go to repository Settings → Pages
2. Under "Source", select "GitHub Actions"
3. The workflow will automatically deploy on the next push to main

If using a different branch or folder structure, update the workflow file accordingly.
