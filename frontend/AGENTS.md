# Frontend development instructions

- Read and follow `../UI_UX_GUIDE.md` before any UI-related work.
- Maintain the guide in the same change when intentional UI conventions change.
- This frontend is separate from `../ims`. Do not edit backend code or configuration as part of frontend work.
- Use existing components, CSS tokens, TypeScript API types, and the shared API client.
- Do not invent APIs, records, unit IDs, stock thresholds, or functional placeholder actions.
- Run `npm run typecheck`, `npm run lint`, `npm test`, `npm run test:e2e`, and `npm run build` for behavioral changes; verify changed layouts on desktop and mobile.
- Test mutations with intercepted API responses. Live integration checks are read-only.

