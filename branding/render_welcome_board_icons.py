# -*- coding: utf-8 -*-
"""Welcome Board icon candidates — round 1.

Class judgement (LOGO_PLAYBOOK): this MOD registers **no block/item** (pure
client GUI utility), so the playbook's default Subject-Max recipe ("mod の
代表ブロック/アイテムのピクセルアート") has no existing texture to crop from
(cf. mod-054-pinboard-anytime, which crops its real block face). It is still
class (1) original (not an addon, not an isekai-series entry), so Subject-Max
grammar applies: one subject filling most of the frame, flat bg (transparent
or one theme color), no ring/glow/radial/badge, at most a 1px flat drop
shadow. The "subject" here has to be invented — three different concrete
objects standing in for "a screen that greets you the moment you join":

  A. signboard  — a hung message board on a post. Literal reading of the
     mod's own name ("Welcome *Board*"). 3 abstracted message lines (a
     document/notice pictogram, not literal text — unreadable at 48px
     anyway) instead of a decorative motif.
  B. open door   — a door ajar with warm light spilling from the gap and a
     striped welcome mat below. Reads as "arriving somewhere for the first
     time", independent of the word "board".
  C. dialog panel — a miniature of the actual in-game screen (title bar +
     2 body lines + 1 button bar) on a near-white card. The most literal
     "this is what the mod puts on your screen" reading.

Each uses a flat rounded-square background in its own theme color (not
picked to dodge any reference set — LOGO_PLAYBOOK's 2026-07-30 color note
applies: choose from the subject, not from collision-avoidance) and a
flat, multi-tone (not gradient) subject with at most one flat shade band
for a bevel cue. No purple/blue accent, no glow/radial-gradient, no dot
badges (negative_list.md §0).

Outputs:
  branding/icon-candidates/welcome_board_A_signboard.png
  branding/icon-candidates/welcome_board_B_door.png
  branding/icon-candidates/welcome_board_C_panel.png
  branding/icon-candidates/_contact_sheet.png   (96/48 legibility insets)
"""

import os
from PIL import Image, ImageDraw

SS, OUT = 4, 512
N = OUT * SS
HERE = os.path.dirname(__file__)
OUTDIR = os.path.join(HERE, "icon-candidates")
os.makedirs(OUTDIR, exist_ok=True)


def rounded_bg(rgb, size=N, radius_frac=0.20):
    big = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    d = ImageDraw.Draw(big)
    radius = int(size * radius_frac)
    d.rounded_rectangle([0, 0, size, size], radius=radius, fill=rgb + (255,))
    return big


def cx_cy():
    return (N - 1) / 2.0, (N - 1) / 2.0


def flat_shadow(canvas, mask, dx, dy, alpha=70):
    """Single flat drop shadow from an alpha mask (Subject-Max's only
    permitted decoration)."""
    sh = Image.new("RGBA", (N, N), (0, 0, 0, 0))
    sh.paste((0, 0, 0, alpha), (dx, dy), mask)
    return Image.alpha_composite(canvas, sh)


# ---------------------------------------------------------------------------
# A. signboard — board (rounded rect) + 3 message lines + support post.
#    BG: sage green (fresh-start / new-arrival color, not wood — kept
#    apart from the board's own material tones for contrast).
# ---------------------------------------------------------------------------
BG_A = (104, 163, 92)
BOARD_FILL = (203, 150, 90)
BOARD_SHADE = (163, 113, 61)
BOARD_OUTLINE = (61, 40, 24)
LINE_COLOR = (250, 236, 210)
POST_FILL = (94, 61, 34)
POST_SHADE = (72, 45, 22)


def draw_signboard(canvas):
    cx, cy = cx_cy()
    subj = Image.new("RGBA", (N, N), (0, 0, 0, 0))
    d = ImageDraw.Draw(subj)

    board_w, board_h = 0.72 * N, 0.44 * N
    board_top = cy - 0.32 * N
    board_left = cx - board_w / 2
    board_right = board_left + board_w
    board_bottom = board_top + board_h
    radius = int(0.05 * N)

    d.rounded_rectangle(
        [board_left, board_top, board_right, board_bottom],
        radius=radius,
        fill=BOARD_FILL + (255,),
    )
    # flat bevel band along the bottom edge (no gradient — one extra flat tone)
    band_h = board_h * 0.16
    d.rectangle(
        [board_left, board_bottom - band_h, board_right, board_bottom],
        fill=BOARD_SHADE + (255,),
    )
    d.rounded_rectangle(
        [board_left, board_top, board_right, board_bottom],
        radius=radius,
        outline=BOARD_OUTLINE + (255,),
        width=int(0.014 * N),
    )

    # 3 abstracted message lines, decreasing width (notice/document pictogram)
    line_h = 0.038 * N
    line_y = [
        board_top + board_h * 0.28,
        board_top + board_h * 0.48,
        board_top + board_h * 0.66,
    ]
    widths = [0.78, 0.58, 0.40]
    for y, w_frac in zip(line_y, widths):
        lw = board_w * w_frac
        lx = cx - lw / 2
        d.rounded_rectangle(
            [lx, y, lx + lw, y + line_h],
            radius=int(line_h / 2),
            fill=LINE_COLOR + (255,),
        )

    # support post, centered below the board
    post_w, post_h = 0.115 * N, 0.30 * N
    post_left = cx - post_w / 2
    post_top = board_bottom - 0.01 * N
    d.rectangle(
        [post_left, post_top, post_left + post_w, post_top + post_h],
        fill=POST_FILL + (255,),
    )
    d.rectangle(
        [cx, post_top, post_left + post_w, post_top + post_h], fill=POST_SHADE + (255,)
    )

    mask = subj.split()[3]
    canvas = flat_shadow(canvas, mask, int(0.012 * N), int(0.014 * N))
    canvas.alpha_composite(subj)
    return canvas


# ---------------------------------------------------------------------------
# B. open door — frame + door-ajar panel + light gap + knob + mat.
#    BG: dawn orange (the moment of arrival), independent of "board" reading.
# ---------------------------------------------------------------------------
BG_B = (219, 140, 74)
FRAME_COLOR = (58, 40, 26)
PANEL_FILL = (176, 112, 58)
PANEL_SHADE = (136, 84, 42)
GAP_LIGHT = (255, 231, 176)
KNOB_COLOR = (43, 28, 16)
MAT_A = (136, 84, 42)
MAT_B = (255, 231, 176)


def draw_door(canvas):
    cx, cy = cx_cy()
    subj = Image.new("RGBA", (N, N), (0, 0, 0, 0))
    d = ImageDraw.Draw(subj)

    frame_w, frame_h = 0.56 * N, 0.76 * N
    frame_top = cy - 0.40 * N
    frame_left = cx - frame_w / 2
    frame_right = frame_left + frame_w
    frame_bottom = frame_top + frame_h
    stroke = 0.045 * N

    d.rounded_rectangle(
        [frame_left, frame_top, frame_right, frame_bottom],
        radius=int(0.03 * N),
        outline=FRAME_COLOR + (255,),
        width=int(stroke),
    )

    inner_left = frame_left + stroke
    inner_right = frame_right - stroke
    inner_top = frame_top + stroke
    inner_bottom = frame_bottom - stroke
    inner_w = inner_right - inner_left

    gap_w = inner_w * 0.30
    panel_right = inner_right - gap_w

    d.rectangle(
        [inner_left, inner_top, panel_right, inner_bottom], fill=PANEL_FILL + (255,)
    )
    shade_w = (panel_right - inner_left) * 0.16
    d.rectangle(
        [panel_right - shade_w, inner_top, panel_right, inner_bottom],
        fill=PANEL_SHADE + (255,),
    )
    d.rectangle(
        [panel_right, inner_top, inner_right, inner_bottom], fill=GAP_LIGHT + (255,)
    )

    knob_r = 0.018 * N
    knob_cx = panel_right - (panel_right - inner_left) * 0.14
    knob_cy = cy + 0.02 * N
    d.rectangle(
        [knob_cx - knob_r, knob_cy - knob_r, knob_cx + knob_r, knob_cy + knob_r],
        fill=KNOB_COLOR + (255,),
    )

    # welcome mat, striped, just below the doorway
    mat_top = frame_bottom + 0.03 * N
    mat_h = 0.075 * N
    n_stripes = 6
    stripe_w = frame_w / n_stripes
    for i in range(n_stripes):
        col = MAT_A if i % 2 == 0 else MAT_B
        x0 = frame_left + i * stripe_w
        d.rectangle([x0, mat_top, x0 + stripe_w, mat_top + mat_h], fill=col + (255,))

    mask = subj.split()[3]
    canvas = flat_shadow(canvas, mask, int(0.012 * N), int(0.014 * N))
    canvas.alpha_composite(subj)
    return canvas


# ---------------------------------------------------------------------------
# C. dialog panel — miniature of the actual in-game screen (title bar,
#    2 body lines, 1 button bar) on a near-white card.
#    BG: dusty teal (a calm, informational color; not blue/purple, not dark).
# ---------------------------------------------------------------------------
BG_C = (79, 148, 150)
CARD_FILL = (250, 247, 240)
TITLE_BAR = (63, 92, 93)
BODY_LINE = (196, 191, 181)
BUTTON_FILL = (63, 92, 93)
BUTTON_LABEL = (250, 247, 240)


def draw_panel(canvas):
    cx, cy = cx_cy()
    subj = Image.new("RGBA", (N, N), (0, 0, 0, 0))
    d = ImageDraw.Draw(subj)

    card_w, card_h = 0.74 * N, 0.66 * N
    card_left = cx - card_w / 2
    card_top = cy - card_h / 2
    card_right = card_left + card_w
    card_bottom = card_top + card_h
    radius = int(0.045 * N)

    d.rounded_rectangle(
        [card_left, card_top, card_right, card_bottom],
        radius=radius,
        fill=CARD_FILL + (255,),
    )

    # title bar (rounded only at the top, achieved by drawing a rect that
    # overlaps the card's rounded top corners from below the radius line)
    bar_h = card_h * 0.20
    d.rectangle(
        [card_left, card_top + radius * 0.5, card_right, card_top + bar_h],
        fill=TITLE_BAR + (255,),
    )
    d.rounded_rectangle(
        [card_left, card_top, card_right, card_top + bar_h * 1.4],
        radius=radius,
        fill=TITLE_BAR + (255,),
    )
    # re-cover the bar's own bottom-rounded corners so only the top stays round
    d.rectangle(
        [card_left, card_top + bar_h, card_right, card_top + bar_h * 1.4],
        fill=TITLE_BAR + (255,),
    )

    # 2 body lines
    line_h = card_h * 0.05
    for i, w_frac in enumerate([0.80, 0.55]):
        ly = card_top + bar_h * 1.6 + i * (line_h * 2.1)
        lw = card_w * 0.82 * w_frac / 0.82 if False else card_w * w_frac * 0.9
        lx = card_left + (card_w - lw) / 2
        d.rounded_rectangle(
            [lx, ly, lx + lw, ly + line_h],
            radius=int(line_h / 2),
            fill=BODY_LINE + (255,),
        )

    # 1 button bar near the bottom
    btn_w, btn_h = card_w * 0.42, card_h * 0.13
    btn_left = cx - btn_w / 2
    btn_top = card_bottom - btn_h * 1.6
    d.rounded_rectangle(
        [btn_left, btn_top, btn_left + btn_w, btn_top + btn_h],
        radius=int(btn_h / 2),
        fill=BUTTON_FILL + (255,),
    )
    label_w, label_h = btn_w * 0.36, btn_h * 0.22
    d.rounded_rectangle(
        [
            cx - label_w / 2,
            btn_top + btn_h / 2 - label_h / 2,
            cx + label_w / 2,
            btn_top + btn_h / 2 + label_h / 2,
        ],
        radius=int(label_h / 2),
        fill=BUTTON_LABEL + (255,),
    )

    mask = subj.split()[3]
    canvas = flat_shadow(canvas, mask, int(0.010 * N), int(0.012 * N), alpha=55)
    canvas.alpha_composite(subj)
    return canvas


CANDS = [
    (
        "welcome_board_A_signboard",
        BG_A,
        draw_signboard,
        "掲示板(サインボード)。柱に掲げた板+抽象化した3本のメッセージ線。MOD名「Welcome *Board*」の直訳。地色は板の木目と衝突しないセージグリーン(新規参加=芽吹きのイメージ)。",
    ),
    (
        "welcome_board_B_door",
        BG_B,
        draw_door,
        "開いた扉。扉の隙間から暖色の光が漏れ、下に縞のウェルカムマット。「初めて到着する瞬間」を board という語に頼らず表現。地色は夜明けのオレンジ。",
    ),
    (
        "welcome_board_C_panel",
        BG_C,
        draw_panel,
        "実装済みGUI画面のミニチュア。タイトルバー+本文2行+ボタン1個をカード状に凝縮した、最も直訳的な意匠。地色は落ち着いたダスティティール(紫・青は使わない)。",
    ),
]

finals = {}
for name, bg, draw_fn, _desc in CANDS:
    canvas = rounded_bg(bg)
    canvas = draw_fn(canvas)
    small = canvas.resize((OUT, OUT), Image.LANCZOS)
    small.save(os.path.join(OUTDIR, f"{name}.png"))
    finals[name] = small
    print("saved", name)

# --- contact sheet: 1 row x 3, big tile + 96/48 legibility insets ---
TILE, PAD, LABEL = 320, 24, 26
cols, rows = 3, 1
sw = PAD + cols * (TILE + PAD)
sh = PAD + rows * (TILE + LABEL + PAD)
sheet = Image.new("RGBA", (sw, sh), (52, 54, 60, 255))
sd = ImageDraw.Draw(sheet)

for i, (name, _bg, _fn, _desc) in enumerate(CANDS):
    t = finals[name]
    cx_ = PAD + (i % cols) * (TILE + PAD)
    cy_ = PAD + LABEL + (i // cols) * (TILE + LABEL + PAD)
    sheet.alpha_composite(t.resize((TILE, TILE), Image.LANCZOS), (cx_, cy_))
    i96 = t.resize((96, 96), Image.LANCZOS)
    i48 = t.resize((48, 48), Image.LANCZOS)
    sheet.alpha_composite(i96, (cx_ + TILE - 96 - 6, cy_ + TILE - 96 - 6))
    sheet.alpha_composite(i48, (cx_ + TILE - 96 - 6 - 48 - 8, cy_ + TILE - 48 - 6))
    sd.rectangle(
        [cx_ + TILE - 96 - 6, cy_ + TILE - 96 - 6, cx_ + TILE - 6, cy_ + TILE - 6],
        outline=(150, 150, 150),
    )
    sd.rectangle(
        [
            cx_ + TILE - 96 - 6 - 48 - 8,
            cy_ + TILE - 48 - 6,
            cx_ + TILE - 96 - 6 - 8,
            cy_ + TILE - 6,
        ],
        outline=(150, 150, 150),
    )
    sd.text((cx_ + 2, cy_ - 20), name, fill=(230, 230, 230))

sheet.convert("RGB").save(os.path.join(OUTDIR, "_contact_sheet.png"))
print("saved _contact_sheet.png")
