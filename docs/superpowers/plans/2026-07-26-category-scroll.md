# Category Page Independent Scroll Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make the mini program category page's left category list and right product area scroll independently, and reset the right product area to the top when a left-side category is selected.

**Architecture:** Keep the current two-column category page architecture. Use WeChat Mini Program `scroll-view` ownership for each column, bind the right `scroll-view` to a `mainScrollTop` data field, and make page/container CSS prevent the outer page from becoming the scroll owner.

**Tech Stack:** WeChat Mini Program WXML/WXSS/JavaScript, existing category/product API modules, manual verification in the WeChat Mini Program simulator.

## Global Constraints

- Preserve the current visual design of the category page.
- Preserve existing category/product interactions: subcategory expansion, subcategory filtering, product grid/list toggle, product navigation, and search tab behavior.
- Do not change category or product API behavior.
- Do not make the right-side subcategory panel sticky.
- Do not change tab bar or page navigation behavior.
- Work from `/Users/yangjingquan/Documents/test/shop`; this session is already in an isolated git worktree.

---

## File Structure

- Modify `miniap/pages/category/index.wxml`: bind the right-side `scroll-view.main` to `mainScrollTop` and enable scroll containment attributes supported by mini program `scroll-view`.
- Modify `miniap/pages/category/index.wxss`: lock the page and two-column layout to the viewport so scrolling happens inside `.side` and `.main`, not the outer page.
- Modify `miniap/pages/category/index.js`: add `mainScrollTop` state and reset it when switching top categories.
- No new production files are required.
- No automated test file is added because this repository has no package-level test harness in the current worktree; verification is manual in the mini program simulator.

---

### Task 1: Contain Category Page Scrolling in the Two Scroll Views

**Files:**
- Modify: `miniap/pages/category/index.wxml:8-23`
- Modify: `miniap/pages/category/index.wxss:1-116`

**Interfaces:**
- Consumes: Existing WXML classes `cat-page`, `layout`, `side`, and `main`.
- Produces: A layout where `.side` and `.main` each own vertical scroll within the fixed `.layout` height.

- [ ] **Step 1: Record the current behavior before editing**

Open the mini program simulator to the category tab. Use the current code and observe:

1. Drag inside the left category list.
2. Drag inside the right product area.
3. Note whether the whole page moves or whether either column moves the other column.

Expected before the fix: at least one drag path allows the page/content to feel linked or allows outer-page movement, matching the reported screenshot behavior.

- [ ] **Step 2: Update the scroll-view attributes**

Edit `miniap/pages/category/index.wxml` so the left and right vertical scroll containers explicitly use enhanced scrolling, hide native scrollbars, and prevent scroll chaining where supported.

Replace:

```xml
<scroll-view class="side" scroll-y>
```

with:

```xml
<scroll-view class="side" scroll-y enhanced show-scrollbar="false" bounces="false">
```

Replace:

```xml
<scroll-view class="main" scroll-y>
```

with:

```xml
<scroll-view class="main" scroll-y enhanced show-scrollbar="false" bounces="false">
```

- [ ] **Step 3: Lock the page to the viewport**

Edit `miniap/pages/category/index.wxss`.

Replace the existing `page` block:

```css
page {
  background: #f7f4ed;
}
```

with:

```css
page {
  height: 100%;
  overflow: hidden;
  background: #f7f4ed;
}
```

Replace the existing `.cat-page` block:

```css
.cat-page {
  min-height: 100vh;
  padding: 20rpx 24rpx calc(112rpx + env(safe-area-inset-bottom));
  box-sizing: border-box;
  background: #f7f4ed;
  color: #17191f;
}
```

with:

```css
.cat-page {
  height: 100vh;
  overflow: hidden;
  padding: 20rpx 24rpx calc(112rpx + env(safe-area-inset-bottom));
  box-sizing: border-box;
  background: #f7f4ed;
  color: #17191f;
}
```

- [ ] **Step 4: Make scroll containers clip their own content**

In `miniap/pages/category/index.wxss`, replace the existing `.side` block:

```css
.side {
  height: 100%;
  padding: 16rpx 12rpx;
  border-radius: 16rpx;
  background: rgba(255, 255, 255, 0.68);
  box-shadow: inset 0 0 0 1rpx rgba(23, 25, 31, 0.04);
  box-sizing: border-box;
}
```

with:

```css
.side {
  height: 100%;
  overflow: hidden;
  padding: 16rpx 12rpx;
  border-radius: 16rpx;
  background: rgba(255, 255, 255, 0.68);
  box-shadow: inset 0 0 0 1rpx rgba(23, 25, 31, 0.04);
  box-sizing: border-box;
}
```

Replace the existing `.main` block:

```css
.main {
  min-width: 0;
  height: 100%;
  box-sizing: border-box;
}
```

with:

```css
.main {
  min-width: 0;
  height: 100%;
  overflow: hidden;
  box-sizing: border-box;
}
```

- [ ] **Step 5: Run syntax/search checks**

Run these commands from `/Users/yangjingquan/Documents/test/shop`:

```bash
git diff -- miniap/pages/category/index.wxml miniap/pages/category/index.wxss
```

Expected: only the intended `scroll-view` attributes and CSS containment changes appear.

- [ ] **Step 6: Verify independent scrolling manually**

Open the category page in the mini program simulator and verify:

1. Drag the left category column down several items.
2. Confirm the right product area stays still.
3. Drag the right product area down several products.
4. Confirm the left category column stays still.
5. Confirm the page header/search area and tab bar do not move during either drag.

Expected: left and right scroll independently inside their own columns.

- [ ] **Step 7: Commit Task 1**

```bash
git add miniap/pages/category/index.wxml miniap/pages/category/index.wxss
git commit -m "fix: contain category page column scrolling

Co-Authored-By: Claude <noreply@anthropic.com>"
```

---

### Task 2: Reset Right Product Area on Top Category Selection

**Files:**
- Modify: `miniap/pages/category/index.wxml:23`
- Modify: `miniap/pages/category/index.js:5-179`

**Interfaces:**
- Consumes: Existing `switchTopCategory(id: number|string): void`, `renderTop(topId: number, presetCategoryId?: number): void`, and right-side `scroll-view.main`.
- Produces: Data field `mainScrollTop: number` bound to the right scroll-view; method `resetMainScroll(): void` used before rendering a newly selected top category.

- [ ] **Step 1: Bind the right scroll-view to `mainScrollTop`**

Edit `miniap/pages/category/index.wxml`.

Replace the right scroll-view opening tag created in Task 1:

```xml
<scroll-view class="main" scroll-y enhanced show-scrollbar="false" bounces="false">
```

with:

```xml
<scroll-view class="main" scroll-y enhanced show-scrollbar="false" bounces="false" scroll-top="{{ mainScrollTop }}">
```

- [ ] **Step 2: Add `mainScrollTop` to page data**

Edit `miniap/pages/category/index.js`.

Replace the end of the `data` block:

```js
    productViewMode: 'list',
    categoryExpanded: false,
    loading: false,
    productLoading: false,
```

with:

```js
    productViewMode: 'list',
    categoryExpanded: false,
    mainScrollTop: 0,
    loading: false,
    productLoading: false,
```

- [ ] **Step 3: Add a focused reset helper**

In `miniap/pages/category/index.js`, insert this method between `hasOriginalPrice` and `switchTopCategory`:

```js
  resetMainScroll() {
    this.setData({ mainScrollTop: 1 })
    wx.nextTick(() => {
      this.setData({ mainScrollTop: 0 })
    })
  },
```

The temporary value `1` forces `scroll-view` to observe repeated category switches even when the previous stored value was already `0`.

- [ ] **Step 4: Call the helper when the top category changes**

In `miniap/pages/category/index.js`, replace the existing `switchTopCategory` method:

```js
  switchTopCategory(id) {
    const topId = this.resolveTopId(this.data.topCats, Number(id))
    if (!topId || !this.data.topCats.find((c) => c.id === topId)) return
    if (topId === this.data.activeTopId) return
    this.setData({ activeTopId: topId })
    this.renderTop(topId)
  },
```

with:

```js
  switchTopCategory(id) {
    const topId = this.resolveTopId(this.data.topCats, Number(id))
    if (!topId || !this.data.topCats.find((c) => c.id === topId)) return
    if (topId === this.data.activeTopId) return
    this.resetMainScroll()
    this.setData({ activeTopId: topId })
    this.renderTop(topId)
  },
```

- [ ] **Step 5: Run syntax/search checks**

Run these commands from `/Users/yangjingquan/Documents/test/shop`:

```bash
git diff -- miniap/pages/category/index.wxml miniap/pages/category/index.js
```

Expected:

- `scroll-top="{{ mainScrollTop }}"` appears on only the right `.main` scroll-view.
- `mainScrollTop: 0` appears in `data`.
- `resetMainScroll()` appears once.
- `switchTopCategory()` calls `this.resetMainScroll()` only after confirming the target top category is valid and different.

- [ ] **Step 6: Verify top-category reset manually**

Open the category page in the mini program simulator and verify:

1. Scroll the right product area downward.
2. Tap a different top category in the left column.
3. Confirm the right product area returns to the top.
4. Scroll the right product area downward again.
5. Tap another different top category.
6. Confirm the right product area returns to the top again.
7. Tap the currently active left category and confirm nothing unexpected jumps or reloads.

Expected: right-side reset happens for every real top-category change and does not happen for tapping the already-active category.

- [ ] **Step 7: Commit Task 2**

```bash
git add miniap/pages/category/index.wxml miniap/pages/category/index.js
git commit -m "fix: reset category products scroll on tab switch

Co-Authored-By: Claude <noreply@anthropic.com>"
```

---

### Task 3: Final Regression Verification

**Files:**
- Verify: `miniap/pages/category/index.wxml`
- Verify: `miniap/pages/category/index.wxss`
- Verify: `miniap/pages/category/index.js`

**Interfaces:**
- Consumes: Task 1 independent scroll containment and Task 2 `mainScrollTop` reset behavior.
- Produces: Verified category page behavior matching the approved design.

- [ ] **Step 1: Inspect the final diff against the implementation scope**

Run:

```bash
git diff HEAD~2..HEAD -- miniap/pages/category/index.wxml miniap/pages/category/index.wxss miniap/pages/category/index.js
```

Expected: the final implementation changes only the category page layout/scroll files and only adds scroll containment plus right-side scroll reset behavior.

- [ ] **Step 2: Verify left/right independent scrolling**

In the mini program simulator:

1. Open the category tab.
2. Drag the left category column from top to lower categories.
3. Confirm the right product area does not move.
4. Drag the right product area from top to lower products.
5. Confirm the left category column does not move.
6. Confirm the top search area remains fixed and visible.

Expected: the two red-boxed regions from the user's screenshot scroll independently.

- [ ] **Step 3: Verify category switching resets right scroll**

In the mini program simulator:

1. Scroll the right product area down.
2. Tap a different left-side category.
3. Confirm the right area returns to its top, showing the category panel and section header at the top of the right column.
4. Repeat with another category.

Expected: every changed left category resets the right scroll position to the top.

- [ ] **Step 4: Verify existing right-side controls still work**

In the mini program simulator:

1. Tap `展开` and confirm subcategories render as a grid.
2. Tap `收起` and confirm subcategories return to the horizontal chip row.
3. Tap a subcategory chip and confirm the selected chip changes and products update.
4. Tap the product view toggle and confirm list/grid mode changes.
5. Tap a product and confirm navigation to product detail still works.

Expected: all existing controls behave as they did before this change.

- [ ] **Step 5: Check repository status**

Run:

```bash
git status --short
```

Expected: clean working tree after the two task commits. If manual verification notes were recorded in local scratch files, delete them before this check.
