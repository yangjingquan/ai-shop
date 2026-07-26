# Category Independent Scroll Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make the mini-program category page keep its top bar fixed while the left category list and right product content scroll independently without visible vertical scrollbars.

**Architecture:** Keep the existing page structure and JavaScript behavior. Convert the category page shell into a fixed-height viewport container, keep the existing two-column grid as the bounded scroll area, and configure both existing vertical `scroll-view` components to hide their scrollbars.

**Tech Stack:** Native WeChat Mini Program WXML/WXSS, existing `scroll-view` components, existing category page JavaScript.

## Global Constraints

- Modify only `miniap/pages/category/index.wxml` and `miniap/pages/category/index.wxss` for product behavior.
- Do not change product loading logic.
- Do not change category selection logic.
- Do not redesign category cards, product cards, or the top search bar.
- Do not make the right-side secondary category panel fixed separately from the product list.
- Preserve the current left column width of `176rpx` and the right column flexible width.
- Preserve bottom tabbar and safe-area spacing so content is not hidden.

---

## File Structure

- Modify `miniap/pages/category/index.wxml`
  - Responsibility: declares the category page layout and scroll-view behavior.
  - Change: add `enhanced` and `show-scrollbar="{{false}}"` to the left `.side` and right `.main` vertical scroll-views.

- Modify `miniap/pages/category/index.wxss`
  - Responsibility: styles the category page layout and scroll containers.
  - Change: make `.cat-page` a fixed viewport shell, prevent page-level overflow, preserve `.layout` as the bounded remaining-height grid, and add scrollbar hiding fallback selectors for `.side` and `.main`.

No JavaScript files should change.

---

### Task 1: Bound the category page scroll containers

**Files:**
- Modify: `miniap/pages/category/index.wxml:8-23`
- Modify: `miniap/pages/category/index.wxss:5-60`

**Interfaces:**
- Consumes: Existing WXML classes `.cat-page`, `.layout`, `.side`, and `.main`.
- Produces: A fixed page shell where `.side` and `.main` remain the only vertical scroll surfaces below the top bar.

- [ ] **Step 1: Inspect the current category layout**

Read the relevant files before editing:

```bash
python3 - <<'PY'
from pathlib import Path
for path in [
    'miniap/pages/category/index.wxml',
    'miniap/pages/category/index.wxss',
]:
    print(f'--- {path} ---')
    for i, line in enumerate(Path(path).read_text().splitlines(), 1):
        if path.endswith('.wxml') and 1 <= i <= 30:
            print(f'{i}: {line}')
        if path.endswith('.wxss') and 1 <= i <= 70:
            print(f'{i}: {line}')
PY
```

Expected: output shows `.cat-page`, `.layout`, `<scroll-view class="side" scroll-y>`, and `<scroll-view class="main" scroll-y>`.

- [ ] **Step 2: Add a static structure check before implementation**

Create a temporary local check script outside the repo index at `/tmp/check-category-scroll.py`:

```bash
cat > /tmp/check-category-scroll.py <<'PY'
from pathlib import Path

wxml = Path('miniap/pages/category/index.wxml').read_text()
wxss = Path('miniap/pages/category/index.wxss').read_text()

required_wxml = [
    '<scroll-view class="side" scroll-y enhanced show-scrollbar="{{false}}">',
    '<scroll-view class="main" scroll-y enhanced show-scrollbar="{{false}}">',
]
required_wxss = [
    '.cat-page {',
    'height: 100vh;',
    'overflow: hidden;',
    '.side::-webkit-scrollbar,',
    '.main::-webkit-scrollbar {',
    'display: none;',
]

missing = [item for item in required_wxml if item not in wxml]
missing += [item for item in required_wxss if item not in wxss]

if missing:
    print('Missing expected category scroll markers:')
    for item in missing:
        print(f'- {item}')
    raise SystemExit(1)

if 'min-height: 100vh;' in wxss and 'height: 100vh;' not in wxss:
    print('cat-page still only uses min-height without fixed height')
    raise SystemExit(1)

print('category scroll structure OK')
PY
```

- [ ] **Step 3: Run the static check and verify it fails before implementation**

Run:

```bash
python3 /tmp/check-category-scroll.py
```

Expected: FAIL with missing markers including the updated `scroll-view` attributes and `height: 100vh;`.

- [ ] **Step 4: Update both vertical scroll-views to hide native scrollbars**

In `miniap/pages/category/index.wxml`, replace the left and right opening `scroll-view` tags exactly as follows.

Change:

```xml
<scroll-view class="side" scroll-y>
```

To:

```xml
<scroll-view class="side" scroll-y enhanced show-scrollbar="{{false}}">
```

Change:

```xml
<scroll-view class="main" scroll-y>
```

To:

```xml
<scroll-view class="main" scroll-y enhanced show-scrollbar="{{false}}">
```

Do not change any child markup, bindings, `wx:for`, or event handlers.

- [ ] **Step 5: Fix the page shell height and page-level overflow**

In `miniap/pages/category/index.wxss`, update `.cat-page` from:

```css
.cat-page {
  min-height: 100vh;
  padding: 20rpx 24rpx calc(112rpx + env(safe-area-inset-bottom));
  box-sizing: border-box;
  background: #f7f4ed;
  color: #17191f;
}
```

To:

```css
.cat-page {
  height: 100vh;
  padding: 20rpx 24rpx calc(112rpx + env(safe-area-inset-bottom));
  overflow: hidden;
  box-sizing: border-box;
  background: #f7f4ed;
  color: #17191f;
}
```

This makes the page a fixed viewport container and prevents the right product content from making the whole page scroll.

- [ ] **Step 6: Preserve the bounded two-column layout**

Confirm the existing `.layout` block remains:

```css
.layout {
  height: calc(100vh - 236rpx - env(safe-area-inset-bottom));
  min-height: 0;
  margin-top: 28rpx;
  display: grid;
  grid-template-columns: 176rpx 1fr;
  gap: 24rpx;
}
```

If any of these declarations are missing, restore them exactly as shown. Do not change the calculated height unless visual verification proves bottom content is hidden by the tabbar or safe area.

- [ ] **Step 7: Add WXSS scrollbar hiding fallback**

In `miniap/pages/category/index.wxss`, add this block after the `.main` block and before `.category-panel`:

```css
.side::-webkit-scrollbar,
.main::-webkit-scrollbar {
  width: 0;
  height: 0;
  display: none;
}
```

This hides scroll indicators in environments where the WXML `show-scrollbar` attribute alone is insufficient.

- [ ] **Step 8: Run the static check and verify it passes**

Run:

```bash
python3 /tmp/check-category-scroll.py
```

Expected: PASS with:

```text
category scroll structure OK
```

- [ ] **Step 9: Verify no JavaScript changed**

Run:

```bash
git diff -- miniap/pages/category/index.js
```

Expected: no output.

- [ ] **Step 10: Review the product diff**

Run:

```bash
git diff -- miniap/pages/category/index.wxml miniap/pages/category/index.wxss
```

Expected diff contains only:

- `enhanced show-scrollbar="{{false}}"` added to `.side` scroll-view.
- `enhanced show-scrollbar="{{false}}"` added to `.main` scroll-view.
- `.cat-page` changed from `min-height: 100vh;` to `height: 100vh;`.
- `.cat-page` gains `overflow: hidden;`.
- `.side::-webkit-scrollbar, .main::-webkit-scrollbar` fallback block added.

- [ ] **Step 11: Manually verify in WeChat Developer Tools**

Open the project directory `miniap/` in WeChat Developer Tools and inspect the 分类 tab.

Use enough categories/products to make both columns scrollable, then verify:

1. The top bar with “分类” and the search box stays fixed.
2. Swiping the right product area scrolls only the right column.
3. Swiping the left category area scrolls only the left column.
4. Neither vertical column displays an up/down scrollbar.
5. Bottom content is not hidden by the tabbar or safe area.
6. Category switching, subcategory selection, expand/collapse, product view toggle, and product detail navigation still work.

If visual verification shows bottom content hidden, adjust only `.layout` height calculation in `miniap/pages/category/index.wxss` and repeat this step.

- [ ] **Step 12: Commit the implementation**

Run:

```bash
git add miniap/pages/category/index.wxml miniap/pages/category/index.wxss
git commit -m "fix: isolate category page column scrolling"
```

---

## Self-Review

- Spec coverage: Task 1 covers fixed top bar, independent left/right scrolling, hidden vertical scrollbars, safe-area preservation, and unchanged category/product logic.
- Placeholder scan: No TBD, TODO, deferred implementation, or unspecified error handling remains.
- Type/property consistency: WXML attributes use existing Mini Program `scroll-view` attributes; CSS class names match existing `.cat-page`, `.layout`, `.side`, and `.main` classes.
