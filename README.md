# Welcome Board

> Drop one JSON file into your modpack and every player sees a welcome screen the first time they join. No FancyMenu, no Better Compatibility Checker, no other mod required.

This is for modpack authors, not players.

## What it does

Reads `config/welcome_board/welcome.json` and shows a screen on first join: a title, body text, an optional image, and a row of link buttons. It appears once per player and then never again — unless you bump the `revision` number, in which case it shows once more.

```json
{
  "revision": 1,
  "title": "Welcome to Example Pack",
  "body": [
    "Thanks for downloading. A few things before you start:",
    "Crafting recipes are unchanged except where noted on the wiki.",
    "Join our Discord if you run into problems."
  ],
  "buttons": [
    { "text": "Wiki", "url": "https://example.com/wiki", "anchor": "bottom_left" },
    { "text": "Discord", "url": "https://discord.gg/example", "anchor": "bottom_right" }
  ],
  "close_text": "Got it"
}
```

Every field is optional and independently defaulted. A file missing `title` still shows a screen; a malformed button is dropped and the rest still renders. **Nothing you get wrong in this file makes the screen fail to appear** — the log, or `/welcomeboard preview`, tells you what was skipped and why.

`anchor` accepts `top`, `top_left`, `top_right`, `center`, `left`, `right`, `bottom`, `bottom_left`, `bottom_right`. For buttons only the horizontal part matters — buttons always sit in their own row at the bottom of the panel, so they can never overlap your text or the close button.

Limits: 8 buttons, 32 body lines, 64 KB per file. About four buttons fit across the row at a typical panel width, so put the important links first. Anything over a limit is dropped with a logged warning rather than breaking the screen.

## Multiple content files

Any file in `config/welcome_board/` other than `seen.json` is loaded as its own piece of content, keyed by filename. `welcome.json` and `patch_notes.json` can coexist, each with its own read state and its own `revision`.

## Commands

`/welcomeboard preview` opens your own screen on demand without touching read state — use it while you're editing the file. Parser warnings are printed to your chat at the same time, so a typo shows up immediately instead of silently doing nothing. NeoForge only in this version; the screen itself works the same on Fabric.

## Behavior

- Waits about a second after world join, then checks whether another mod's first-join screen (Origins and similar) is already open. If one is, it waits up to three more seconds for it to close before giving up for that session and logging why. It never fights another mod for the screen.
- Read state is tracked per server/world, not per game instance — the same pack on two different servers shows the screen once on each.
- One setting: `enabled`.

## Not in this version

- No server-authoritative content push (the file lives on each client)
- No multi-page navigation inside a single screen
- No commands run from a button — buttons only open a URL
- One image per content file

## Client-side only

There is no server component. Put it in the pack's client mods and you are done.

## Supported

Minecraft 1.21.1 (NeoForge, Fabric) · Minecraft 26.1.2 and 26.2 (NeoForge). Nine languages.

## License

All Rights Reserved. Free to put in any modpack, on any platform, monetised or not - no permission needed, no credit required. Source is published so you can read exactly what it does.
