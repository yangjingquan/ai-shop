# Category Page Natural Height Design

Date: 2026-07-26

## Goal

Change the mini-program category page from fixed-height internal scrolling to natural document flow. The category page should grow according to its data content height, and visible scrollbars should be hidden.

## Selected Approach

Use whole-page natural scrolling.

- Remove the vertical `scroll-view` containers for the left category list and right product area.
- Render both columns as normal `view` containers so their content contributes to page height.
- Keep the horizontal subcategory chip `scroll-view`, because it is a horizontal overflow control rather than a fixed-height vertical scroll region.
- Remove JavaScript state and methods that only existed to reset the right-side internal scroll position.

## Current Behavior

The current page fixes the viewport and contains scrolling inside the category layout:

- `page` and `.cat-page` hide outer overflow.
- `.cat-page` uses viewport height.
- `.layout` computes a fixed remaining height.
- `.side` and `.main` are vertical `scroll-view` containers with `show-scrollbar="false"`.
- The right content uses `scroll-top="{{ mainScrollTop }}"` and JavaScript resets that value on top-category changes.

This conflicts with the new requirement because the data content no longer determines the page height.

## Target Behavior

- The page scrolls naturally as a single document.
- The left category list and right product area are part of normal page layout.
- No fixed viewport height is set for the category page or category layout.
- No vertical internal scrollbar appears for the left or right column.
- The horizontal subcategory chip row can still scroll horizontally without showing a scrollbar.
- Switching a top category updates content only; it does not manipulate an internal right-column scroll position.

## Files and Components

### `miniap/pages/category/index.wxml`

- Replace the left `.side` vertical `scroll-view` with a `view`.
- Replace the right `.main` vertical `scroll-view` with a `view`.
- Remove vertical scroll-specific attributes from those containers:
  - `scroll-y`
  - `enhanced`
  - `show-scrollbar="false"`
  - `bounces="false"`
  - `scroll-top="{{ mainScrollTop }}"`
- Leave the horizontal `.chip-row` `scroll-view` unchanged unless a scrollbar-specific style adjustment is needed.

### `miniap/pages/category/index.wxss`

- Remove fixed-height and containment rules that force internal scrolling:
  - `page { height: 100%; overflow: hidden; }`
  - `.cat-page { height: 100vh; overflow: hidden; }`
  - `.layout { height: calc(...); min-height: 0; }`
  - `.side { height: 100%; overflow: hidden; }`
  - `.main { height: 100%; overflow: hidden; }`
- Keep the two-column layout.
- Let the grid rows stretch naturally based on content.
- Hide visible scrollbars through non-layout-affecting scrollbar rules where supported.
- Preserve safe-area bottom padding on the page container.

### `miniap/pages/category/index.js`

- Remove `mainScrollTop` from page data.
- Remove `resetMainScroll()`.
- Remove calls to `resetMainScroll()` from top-category switching.
- Keep category selection, expansion, subcategory filtering, and product view-mode behavior unchanged.

## Data Flow

1. Page loads the category tree and renders the active top category.
2. User taps a top category.
3. The page updates active category state and renders subcategories/products.
4. The resulting content height changes naturally with the new data.
5. The mini-program page handles vertical scrolling as one page.

## Error Handling

No new error paths are introduced. Existing data-loading fallback behavior remains unchanged.

If category or product data is empty, existing empty-state rendering remains inside `.main` and contributes to natural page height.

## Testing and Verification

Static checks:

- Confirm `.side` and `.main` are no longer vertical `scroll-view` elements.
- Confirm no `mainScrollTop` or `resetMainScroll()` references remain.
- Confirm category page CSS no longer fixes page/layout height or hides outer overflow.

Manual simulator checks:

- Open the category page with enough category/product data to exceed the viewport.
- Confirm the whole page scrolls naturally.
- Confirm left and right columns do not show independent vertical scrollbars.
- Confirm the horizontal subcategory chip row still scrolls horizontally.
- Switch top categories and confirm content updates correctly.
- Toggle grid/list product view and confirm the page height adjusts naturally.

## Out of Scope

- Adding automatic page scroll-to-top when switching categories.
- Redesigning category/product card visuals.
- Changing category or product data shape.
- Changing tab bar or navigation behavior.
