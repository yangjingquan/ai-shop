# Design QA

source visual truth path: `/var/folders/gd/133fk3b92tl_glzpvs2kxf9r0000gn/T/codex-clipboard-9b3b2fbd-54e0-449a-851b-0ed2dad9fe8a.png` (图 2，目标下拉菜单与个人中心)
implementation screenshot path: not captured for the protected backend state; the local preview reached `/login` at `http://127.0.0.1:5180/login`
viewport: local preview screenshot `1280 x 720` CSS viewport; source image `2557 x 559` pixels, resized by the app display to `2048 x 448`; no density normalization applied because the states could not be aligned
state: source is authenticated ERP personal-center page with user menu open; implementation is unauthenticated login page

## Comparison evidence

- Full-view comparison: blocked because the implementation could not reach an authenticated `/admin/profile` or `/merchant/password` route without a user session.
- Focused region comparison: blocked for the same reason; the user menu and password page are protected by the router guard.
- Static implementation checks passed: the new shared page is routed for both roles, the header exposes a click-triggered user dropdown, and the dropdown has `个人中心` and `退出登录` commands.
- Browser console errors checked on the reachable local login page: none.

## Findings

- [P1] Authenticated visual comparison unavailable.
  Location: `/admin/profile`, `/merchant/password`, and the shared header dropdown.
  Evidence: the source requires an authenticated state; the local preview redirects unauthenticated requests to `/login`.
  Impact: exact spacing, menu placement, responsive behavior, and final visual parity cannot be confirmed in-browser.
  Fix: capture the authenticated implementation with a test session and compare the open dropdown and password page at the source viewport.

## Implementation Checklist

- [x] Both roles route to a shared password page.
- [x] Header password and logout buttons are merged into a current-user dropdown.
- [x] Dropdown includes `个人中心` and `退出登录`.
- [x] Password submission keeps role-specific encrypted APIs and invalidates the current session.
- [x] `pnpm build` passes.
- [ ] Authenticated browser screenshot and visual comparison.

## Comparison history

No P0/P1/P2 visual iteration was run because the authenticated implementation state was unavailable.

final result: blocked
