#!/usr/bin/env bash
set -e

echo "================================================="
echo "  Deploying Air Hockey Web Portal to Cloudflare Pages"
echo "  Target: airhockey-web.pages.dev"
echo "================================================="

# Copy latest icons & feature graphics to web folder
cp store_assets/icon_512x512.png web/icon.png
cp store_assets/feature_graphic_1024x500.png web/feature_graphic.png

# Deploy to Cloudflare Pages via Wrangler
npx wrangler pages deploy web --project-name airhockey-web --commit-dirty=true

echo "================================================="
echo "  Deployment complete!"
echo "  Live at: https://airhockey-web.pages.dev"
echo "  Privacy: https://airhockey-web.pages.dev/privacy"
echo "  app-ads: https://airhockey-web.pages.dev/app-ads.txt"
echo "================================================="
