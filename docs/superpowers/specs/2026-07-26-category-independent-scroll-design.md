# Category Page Independent Scroll Design

## Goal

Update the mini-program category page so the top bar stays fixed while the left category list and right product content scroll independently. Hide the vertical scrollbars for both columns.

## Scope

In scope:

- `miniap/pages/category/index.wxml`
- `miniap/pages/category/index.wxss`
- Category page layout and scroll-view attributes only

Out of scope:

- Product loading logic
- Category selection logic
- Visual redesign of category cards, product cards, or the top search bar
- Making the right-side secondary category panel fixed separately from the product list

## Current Context

The category page already has two vertical `scroll-view` elements:

- Left: `.side` for top-level categories
- Right: `.main` for category panel, product section header, product list, and empty state

The requested behavior requires the page itself to stop acting like the scroll container. The two existing `scroll-view` elements should become the only vertical scrolling surfaces under the fixed top bar.

## Design

### Fixed viewport page shell

Set `.cat-page` to a fixed viewport-height shell:

- `height: 100vh`
- `overflow: hidden`
- `box-sizing: border-box`

Keep its current padding, background, and typography. This prevents product content from increasing the page height and causing whole-page scrolling.

### Remaining-height two-column layout

Keep `.layout` as the two-column grid with the current left column width and gap. Its height remains calculated from the viewport minus the top area and bottom tabbar/safe-area space.

The layout must preserve:

- Left column width: `176rpx`
- Right column flexible width
- `height` and `min-height: 0` so child scroll-views can scroll internally instead of forcing parent growth

### Independent scroll views

Update both vertical scroll-views to hide native scrollbars while preserving vertical scrolling:

```xml
<scroll-view class="side" scroll-y enhanced show-scrollbar="{{false}}">
```

```xml
<scroll-view class="main" scroll-y enhanced show-scrollbar="{{false}}">
```

The left list scrolls only when the user interacts with the left column. The right content scrolls only when the user interacts with the right column.

### Scrollbar hiding fallback

Add WXSS fallback selectors for both columns:

```css
.side::-webkit-scrollbar,
.main::-webkit-scrollbar {
  width: 0;
  height: 0;
  display: none;
}
```

This supports environments where the scroll-view attribute alone does not fully hide the indicator.

### Existing behavior preserved

Do not change the JavaScript page state or event handlers. The following must continue working as-is:

- Loading the category tree
- Selecting a top-level category
- Selecting a subcategory
- Expanding/collapsing the subcategory panel
- Switching product list/grid view
- Navigating to product detail

## Success Criteria

1. The top bar with “分类” and the search box stays fixed.
2. Swiping the right product area scrolls only the right column.
3. Swiping the left category area scrolls only the left column.
4. Neither vertical column displays an up/down scrollbar.
5. Bottom content is not hidden by the tabbar or safe area.
6. Existing category and product interactions keep working.

## Verification Plan

- Inspect the category page in WeChat Developer Tools or on a device.
- Populate enough categories and products to make both columns scrollable.
- Swipe the right product area and confirm the left category list remains still.
- Swipe the left category list and confirm the right product area remains still.
- Confirm no vertical scrollbar is visible on either column.
- Confirm category switching and product navigation still work.
