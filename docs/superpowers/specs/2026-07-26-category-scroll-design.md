# Category Page Independent Scroll Design

Date: 2026-07-26

## Goal

Update the mini program category page so the left category list and right product area scroll independently.

The selected behavior is **Option B**:

- Swiping on the left category column scrolls only the category list.
- Swiping on the right product/content column scrolls only the product area.
- Tapping a left-side top category switches the content and resets the right product area to the top.

## Current Context

Relevant files:

- `miniap/pages/category/index.wxml`
- `miniap/pages/category/index.wxss`
- `miniap/pages/category/index.js`

The page already uses two vertical `scroll-view` elements:

- `.side` for the left category navigation
- `.main` for the right content/product area

The design should preserve the current visual layout and existing category/product interactions while tightening the scroll containment and adding right-side scroll reset on top-category changes.

## Proposed Approach

Use the existing two-column structure and make the scroll ownership explicit:

1. Keep the outer page/header structure unchanged.
2. Ensure the `.layout` area has a fixed remaining viewport height.
3. Ensure `.side` and `.main` each occupy the full `.layout` height and use their own vertical scrolling.
4. Prevent the product list from expanding the page height and causing whole-page scrolling.
5. Add a right-side scroll position state in `index.js`, bound to the right `scroll-view` via `scroll-top`.
6. Reset that state when the user taps a left-side top category.

## Interaction Details

### Left Category Scroll

When the user drags inside the left category column, only `.side` should scroll. The right product list must remain at its current scroll position.

### Right Product Scroll

When the user drags inside the right content column, only `.main` should scroll. The left category list must remain at its current scroll position.

### Top Category Tap

When the user taps a left-side top category:

1. The active top category updates.
2. The related subcategories and product list update using the existing data flow.
3. The right content area scrolls back to the top.
4. The category-expanded state remains reset as it does today.

### Subcategory and Product View Controls

Existing behavior remains unchanged:

- Expanding/collapsing right-side subcategory chips
- Selecting a subcategory
- Toggling product grid/list view

## Implementation Notes

Expected implementation points:

- In `index.wxml`, bind the right `.main` scroll-view to a page data field such as `mainScrollTop`.
- In `index.js`, initialize `mainScrollTop` to `0`.
- In the top-category tap handler, set `mainScrollTop` to a non-zero value and then back to `0` if needed to force WeChat Mini Program `scroll-view` to observe repeated resets.
- In `index.wxss`, verify `.page`, `.layout`, `.side`, and `.main` prevent outer-page scrolling and preserve fixed two-column heights.

## Testing Plan

Manual verification in the mini program simulator:

1. Open the category page.
2. Scroll the left category list down and confirm the right product area does not move.
3. Scroll the right product area down and confirm the left category list does not move.
4. While the right area is scrolled down, tap another top category on the left.
5. Confirm the right product area returns to the top and displays the selected category's products.
6. Confirm subcategory expansion, subcategory filtering, and product view-mode toggling still work.

## Out of Scope

- Changing the visual design of the category page.
- Making the right-side subcategory panel sticky.
- Reworking product data loading or category API behavior.
- Changing tab bar or page navigation behavior.
