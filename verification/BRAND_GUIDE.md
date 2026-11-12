# Kissaten family — provisional visual identity

Three independent, offline products share warm paper, rounded readable Japanese-designed type, restrained accent colors and original line artwork. There are no decorative Japanese phrases or copyrighted characters.

| Product | Identity | Mark | Signature mood |
|---|---|---|---|
| Mizupuku | Your little water café. | Rose droplet containing a small glass and a blossom/star detail | Sakura, warm ivory, water and mint |
| Fumipoka | A little place to remember where you left off. | Lavender notebook and rose bookmark with a small star | Paper, bookmarks, lavender and rose |
| Yorulune | Your quiet corner of the universe. | Pale star, pink orbit and indigo field | Indigo evening, pink, sky and warm amber |

## Type and language

Zen Maru Gothic Regular/Bold supports the rounded UI and readable controls; Shippori Mincho supplies editorial notebook/date headings. Original, unmodified font binaries are bundled locally; SIL OFL notices are in each app's assets/licenses folder. Font provenance and glyph checks are recorded in `assets/fonts`. The supplied Japanese fonts cover the tested English/Japanese text; Slovenian extended Latin characters rely on Android fallback. No complete Japanese or Slovenian translation is claimed.

## Tokens and interaction

Spacing follows the existing 4dp rhythm, with 8–12dp control gaps, 16–20dp page padding and 24–32dp larger separations. Rounded shapes retain each app's existing semantic small/medium/large scale. Typical interactive minimums are 48dp, with taller principal actions. Original café illustrations are rendered as Compose Canvas paths, scale with the available area, carry no semantic information and introduce no mandatory motion.

Each repository owns its local `ui/theme/Kissaten.kt` implementation. Material semantic colors feed screens, dialogs and navigation. The theme helper normalizes surface-container colors, chooses black/white button text by relative luminance and adjusts accent tone when needed for 4.5:1 contrast against the surface. The user-selected hue is retained as closely as the contrast constraint allows. Unit tests cover representative custom accents on light, dark and black surfaces; this is not an exhaustive accessibility certification of every text/background pair.

Light, dark, follow-system and OLED appearance choices remain persisted using the existing storage. OLED uses a true black root/surface; colored selection containers are restrained. Existing named palettes and persisted enum/string values are preserved for compatibility, including the older high-contrast themes. Custom accents use the existing HydroNudge and Returnmark preference fields; Yorulune additionally accepts #RRGGBB in its existing backed-up accentPreset field.

## Assets

Each repository's `play-store` folder contains a 512×512 RGB icon, editable icon SVG, a 1024×500 RGB feature graphic, metadata and privacy drafts. Adaptive foreground/background, monochrome vectors and density-specific launcher assets live in app resources. Generated promotional artwork is never treated as runtime testing evidence. Store screenshots are selected only from the running compiled apps and are validated separately.

The names and marks remain provisional. Do not rename repositories, change policy URLs or publish until the owner approves the branding and clearance gates are resolved.
