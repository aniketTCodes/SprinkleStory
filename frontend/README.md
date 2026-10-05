# Sprinkle Story frontend

A standalone React + TypeScript + Vite UI for the existing Sprinkle Story backend. Backend files are not part of this project.

## Run locally

Requires Node.js 22.12+ (or 20.19+) and npm.

```powershell
cd E:\SprinkleStory\frontend
npm ci
npm run dev
```

Open http://localhost:5173. Start the backend yourself; the frontend does not launch it.

To serve the production build locally:

```powershell
npm run build
npm run preview
```

Both servers use port 5173 and fail if occupied. Stop one before starting the other. Ctrl+C stops the server. Vite preview is for local verification, not production hosting.

## Backend connection

The browser calls relative `/api/v1` URLs. Vite proxies `/api` to `http://localhost:8080` in development and preview, avoiding backend CORS changes. To change the target, copy `.env.example` to `.env.local`, set `API_TARGET`, and restart the frontend server. Never put backend credentials in the frontend.

A future hosted deployment must supply its own same-origin API reverse proxy and SPA route fallback.

## Available features

- Products: backend search/filtering, stock display, create/edit, and confirmed disabling.
- Categories and suppliers: browse, create/edit, and confirmed deletion.
- Sales, purchase orders, inventory lots, stock movements, and users/access: Coming soon pages only. They make no API calls.

Units are derived exclusively from the complete product list. An empty catalog has no selectable units, so product creation is blocked. Categories must also exist. This is an existing API limitation; the frontend does not modify the backend or invent IDs.

Product status and quantities are backend-controlled. There is no reactivation or stock-editing action.

## Checks

```powershell
npm run typecheck
npm run lint
npm test
npx playwright install chromium
npm run test:e2e
npm run build
```

Browser tests intercept API requests and never mutate live backend data. They cover filters, units, confirmations, conflicts, supplier writes, retries, placeholders, mobile overflow, and modal keyboard behavior. Screenshots are saved in `test-results`.

## Design maintenance

Read [UI_UX_GUIDE.md](../UI_UX_GUIDE.md) before UI work and follow [AGENTS.md](./AGENTS.md). Update the guide when shared UI conventions change.


For an optional read-only smoke check against your running backend and frontend preview:

```powershell
npm run verify:live
```

This displays live record counts, checks browser errors and viewport overflow, and saves screenshots in `test-results`. All non-GET API requests are blocked during this check.
