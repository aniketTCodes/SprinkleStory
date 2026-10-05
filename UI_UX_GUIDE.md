# Sprinkle Story UI/UX guide

This is the source of truth for all Sprinkle Story UI development. Read it before adding or changing screens. Maintain it alongside intentional design changes; use the existing components and tokens rather than introducing a competing style.

## Purpose and audience

Help a small confectionery store owner keep products, categories, and suppliers organized with minimal training. Prioritize clear labels, readable inventory tables, predictable actions, and truthful data. Use “Products” in the interface instead of internal “SKU” terminology.

## Visual system

- Canvas: warm cream `#faf8f4`; surfaces: white `#ffffff`; sidebar: `#fffdf9`.
- Primary berry: `#89324f`; hover: `#70243e`; primary text: `#352d32`; secondary text: `#786f73`; borders: `#e9e3de`.
- Success: dark green on pale green; warning: dark amber on cream; errors: dark red on pale pink. Never communicate meaning through color alone.
- Use the CSS custom properties in `frontend/src/styles.css`. New shared colors must become tokens.
- Body and controls: system sans-serif; display headings and small brand flourishes: Georgia. Do not load external fonts. Body/control text is 12–14px; page headings 32–42px. Tiny uppercase labels are supplementary, never essential instructions.
- Use a 4px spacing rhythm, typically 8, 12, 16, 20, 24, 32, and 40px. Surfaces use 10px corners, controls 6–7px, dialogs 14px.
- Keep shadows subtle and limited to overlays. Use line icons from Lucide with visible labels or accessible names. Avoid decorative images in inventory workflows.
- Berry marks primary actions. Destructive actions use red and explicit verbs. Keep one primary action in a page header.

## Layout and navigation

- Desktop: persistent left navigation, light top bar, spacious content, and a quiet footer.
- Under 800px: navigation becomes a horizontally scrollable strip; summary cards use two columns. Content remains within the viewport.
- Tables scroll horizontally within a labeled, keyboard-focusable region. Never cause whole-page horizontal scrolling.
- Use three functional routes: Products, Categories, Suppliers. Separate planned workflows under “Up next”: Sales, Purchase orders, Inventory lots, Stock movements, Users & access.
- Preserve visible active navigation state. Support direct URLs, browser history, a default Products route, and a useful missing-page fallback.

## Tables, data, and forms

- Tables have clear column headers, understated separators, readable names, and aligned numeric columns. Show product code below product name.
- Prices use INR via `Intl.NumberFormat('en-IN')`. Quantities retain meaningful fractions and include the actual unit; never sum incompatible units into one inventory total.
- Product summaries use the complete catalog, not the filtered result. Out of stock means active products with quantity at or below zero; do not invent a low-stock threshold.
- Stock is read-only until corresponding APIs exist. Product codes come from the backend.
- Derive units only from the complete product response. Never invent UUIDs or hard-code seed data. Explain why creation is unavailable if there are no units or categories.
- Forms use persistent labels; * denotes required fields. Optional fields are explicitly optional. Preserve entered values after validation or server failures.
- Validate names, product prerequisites, finite nonnegative MRP, and supported Indian mobile formats. Backend conflict messages remain authoritative.
- Disable submissions while pending and guard against duplicate requests. On success close the form, refresh affected data, and announce completion.

## Dialogs and feedback

- Use the shared native dialog wrapper with an accessible title, modal focus containment, Escape dismissal, and focus restoration. Do not allow dismissal during a pending mutation.
- Confirm deletion and product disabling with the record name and consequence. Default focus to Cancel for destructive confirmations.
- Explain that products with stock cannot be disabled and that reactivation is not currently supported. Show in-use deletion conflicts without losing context.
- Provide distinct loading, empty catalog, no-match, success, validation error, and connection-error states. Errors offer a retry action. Never use fake data as an error fallback.
- Status messages use `role="status"`; errors use `role="alert"`. Text and icons support semantic colors.

## Placeholder pages

- Use one shared placeholder layout: page title, short intended purpose, visible “Coming soon” badge, honest unavailable-feature explanation, and “Back to products” link.
- Do not render fabricated records, simulated transactions, misleading counters, or controls implying a feature works.
- Placeholders must not request nonexistent APIs. Technical details about backend implementation belong in development documentation, not shop workflows.

## Accessibility and motion

- Use semantic headings, landmarks, table headers, buttons, and labels. Icon-only buttons need descriptive accessible names including the affected record.
- Provide a skip link, visible keyboard focus, and comfortable control targets (primary controls at least 43px high).
- Keep essential text at WCAG AA contrast (4.5:1 normal text, 3:1 large text). Check new token combinations before use.
- Respect reduced-motion preferences. Avoid animation beyond a restrained loading indicator.
- Verify keyboard navigation, dialog focus, Escape, focus restoration, and small-screen table scrolling.

## Maintenance checklist

Every UI change must follow this guide, use shared components where appropriate, and update the guide if an intentional convention changes. Run type checking, linting, the relevant tests, and a production build. Inspect the changed screen at desktop and mobile sizes. Keep backend files untouched unless a separate task explicitly authorizes backend work.

