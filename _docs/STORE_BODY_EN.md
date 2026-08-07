<!--
KURONAMI store description (Modrinth body / CurseForge description — shared, English).
Follows knowledge/STORE_BODY_FRAMEWORK.md (no version table, function-first hook, facts over adjectives).

Published 2026-08-07. This block records what was actually set on each store; the
body below it is what goes in the description field.

== Store fields as set ==
MODRINTH  (wT3Fx5Nq, slug welcome-board): categories=utility | env: client REQUIRED,
          server UNSUPPORTED (there is no server-side code) | license LicenseRef-All-Rights-Reserved
          | gallery 2 | 4 versions, all environment=client_only, fabric version declares fabric-api
CURSEFORGE (project 1642803): Class=Mods | main category=Utility & QoL | Allow Comments=ON
          | license=All Rights Reserved | distribution=Allow distribution to 3rd party
          (required so modpacks using external launchers do not break)
SUMMARY: Modpack authors drop one JSON file in and players see a welcome screen on first join. No other mods required.
-->

# Welcome Board

Drop a JSON file in your pack's config folder and every player sees a welcome screen the first time they join. No FancyMenu, no Better Compatibility Checker, no other mod required.

This is for modpack authors, not players. It reads `config/welcome_board/welcome.json` and shows a title, body text, an optional image, and a row of link buttons in a screen that appears once per player, then never again — unless you bump a `revision` number, in which case it shows once more.

Client-side only. There is no server component — put it in the pack's client mods and you are done.

**Content file**

```json
{
  "revision": 1,
  "title": "Welcome to Example Pack",
  "body": [
    "Thanks for downloading. A few things before you start:",
    "Crafting recipes are unchanged except where noted on the wiki.",
    "Join our Discord if you run into problems."
  ],
  "image": {
    "id": "welcome_board:textures/gui/logo.png",
    "anchor": "top",
    "width": 128,
    "height": 64
  },
  "buttons": [
    { "text": "Wiki", "url": "https://example.com/wiki", "anchor": "bottom_left" },
    { "text": "Discord", "url": "https://discord.gg/example", "anchor": "bottom_right" }
  ],
  "close_text": "Got it"
}
```

- `anchor` accepts `top`, `top_left`, `top_right`, `center`, `left`, `right`, `bottom`, `bottom_left`, `bottom_right`. Unrecognized or missing values fall back to `center`.
- For buttons, only the horizontal component of the anchor matters — `top_left`/`left`/`bottom_left` all mean "left-aligned", `top_right`/`right`/`bottom_right` all mean "right-aligned", and the rest center. Buttons always sit in their own row at the bottom of the panel; the vertical part of their anchor is ignored.
- Every field is optional and independently defaulted. A file missing `title` still shows a screen titled "Welcome"; a malformed `buttons` entry is dropped and the rest still renders. Nothing you get wrong here makes the screen fail to appear — the pack author's chat (via `/welcomeboard preview`, see below) or the log tells you what was skipped and why.
- Buttons are capped at 8, body text at 32 lines, and the file itself at 64 KB; anything beyond that is dropped with a logged warning instead of breaking the screen. In practice about four buttons fit across the row at a typical panel width — extra ones are dropped from the end, so put the important links first.

**Multiple pages**

Any file in `config/welcome_board/` other than `seen.json` is loaded as its own piece of content, keyed by filename. `welcome.json` and `patch_notes.json` can coexist and are tracked separately — each has its own read state and its own `revision`.

**Commands**

`/welcomeboard preview` opens your own screen on demand, using whatever is currently in `welcome.json`, without touching read state — use it to check a file while you're editing it. Any warnings the parser produced (bad JSON, a dropped button, a missing field) are printed to your chat at the same time, so a typo shows up immediately instead of silently doing nothing.

The command is NeoForge-only in this version. On Fabric the screen itself works the same; only the preview command is missing.

**Behavior notes**

- Waits about a second after world join, then checks whether another mod's own first-join screen (Origins and similar) is already open. If one is, it waits up to 3 more seconds for it to close before giving up for that session and logging why — it never fights another mod for the screen.
- Read state is tracked per server/world, not per game instance: the same pack on two different servers shows the welcome screen once on each.
- Config has one setting: `enabled`.

**Not in this version**

- No server-authoritative content push (the file lives on each client)
- No multi-page navigation inside a single screen
- No commands run from a button — buttons only open a URL
- One image per content file

Install on the client. No dependencies, and nothing to install server-side.

All Rights Reserved. Free to put in any modpack, on any platform, monetised or not - no permission needed, no credit required. Source is published so you can read exactly what it does.

Source and issues: https://github.com/KURONAMI333/welcome-board
