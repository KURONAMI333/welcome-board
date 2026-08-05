# -*- coding: utf-8 -*-
"""Welcome Board icon round E -- direction reset after STEP1 observation.

Evidence basis (see STEP1 report in the task response, not duplicated here):
  - E1 "hi_text": Serilum's welcome-message / first-join-message icons (40-50万 DL)
    solve "what does this mod do" by literally spelling the function as bold
    text on a flat single-color background (measured bbox ~80%, pixel-fill only
    ~18-22% because strokes are thin -- lots of visible bg despite wide bbox).
  - E2 "signpost_notice": welcomescreen (108万 DL) proves a wooden sign silhouette
    reads as "board/notice" but its icon fills the frame edge-to-edge (bbox=100%,
    fill=81%) -- the exact "no visible margin" flaw kura flagged in our own
    round 3. Reused here at a corrected scale (~55-65% bbox) with a Minecraft-
    native "!" notice glyph (villager trade/profession UI convention) instead of
    the ambiguous carved smiley from round 2.
  - E3 "door_ajar": round 1's welcome_board_B_door.png tried a door but was
    rejected for flat-vector execution (smooth rounded corners, AA fill, no
    pixel grid) -- not for the door concept. Re-executed here in hard-edged
    NEAREST pixel art with a different composition (ajar + light spill + mat,
    not a closed door + window strip) and corrected margin.

All three keep discrete 3-4 tone shading, hard edges, no gradients/AA (per
CRAFT_TEXTURE_RULES cross-cutting principle 1) and a single flat-color
background per subject (LOGO_PLAYBOOK Subject-Max doctrine).
"""

import hashlib

import numpy as np
from PIL import Image, ImageDraw

OUT = "icon-candidates/_pixel_render_work"
FINAL = "C:/Users/naoki/dev/projects/minecraft-mod-dev/mod-062-welcome-board/branding/icon-candidates"


def hexc(h: str) -> tuple[int, int, int, int]:
    h = h.lstrip("#")
    return (int(h[0:2], 16), int(h[2:4], 16), int(h[4:6], 16), 255)


def det_jitter(x: int, y: int, seed: str, amp: int) -> int:
    key = f"{seed}_{x}_{y}".encode()
    v = int(hashlib.md5(key).hexdigest()[:8], 16)
    return (v % (2 * amp + 1)) - amp


def clamp(v: int) -> int:
    return max(0, min(255, v))


def shade(color: tuple[int, int, int, int], delta: int) -> tuple[int, int, int, int]:
    r, g, b, a = color
    return (clamp(r + delta), clamp(g + delta), clamp(b + delta), a)


def new_canvas(w: int, h: int) -> np.ndarray:
    return np.zeros((h, w, 4), dtype=np.uint8)


def to_image(arr: np.ndarray) -> Image.Image:
    return Image.fromarray(arr, "RGBA")


def rect(arr: np.ndarray, x0: int, y0: int, x1: int, y1: int, color) -> None:
    arr[y0:y1, x0:x1] = color


def rect_jitter(
    arr: np.ndarray, x0: int, y0: int, x1: int, y1: int, base, amp: int, jseed: str
) -> None:
    for yy in range(y0, y1):
        for xx in range(x0, x1):
            arr[yy, xx] = shade(base, det_jitter(xx, yy, jseed, amp))


def compose_on_bg(
    subject_img: Image.Image,
    subject_scale: int,
    bg_color,
    canvas: int = 512,
    radius_ratio: float = 0.20,
) -> Image.Image:
    sw, sh = subject_img.size
    disp_w, disp_h = sw * subject_scale, sh * subject_scale
    subj_big = subject_img.resize((disp_w, disp_h), Image.NEAREST)

    base = Image.new("RGBA", (canvas, canvas), (0, 0, 0, 0))
    r = int(canvas * radius_ratio)
    mask = Image.new("L", (canvas, canvas), 0)
    d = ImageDraw.Draw(mask)
    d.rounded_rectangle([0, 0, canvas - 1, canvas - 1], radius=r, fill=255)
    bgfill = Image.new("RGBA", (canvas, canvas), bg_color)
    base = Image.composite(bgfill, base, mask)

    px = (canvas - disp_w) // 2
    py = (canvas - disp_h) // 2
    base.alpha_composite(subj_big, (px, py))
    return base, (disp_w, disp_h, canvas)


def report_occupancy(name: str, disp_w: int, disp_h: int, canvas: int) -> None:
    bbox_pct = 100 * (disp_w * disp_h) / (canvas * canvas)
    print(
        f"{name}: subject bbox = {bbox_pct:.1f}% of frame (disp {disp_w}x{disp_h} / canvas {canvas})"
    )


# ============================================================
# E1: "Hi!" bold pixel text mark on flat warm gold background
# ============================================================

BG_E1 = "#E0A030"
INK_E1 = hexc("#FBF3DE")
SHADOW_E1 = hexc("#6B4413")


def build_e1() -> tuple[Image.Image, tuple[int, int, int]]:
    W, H = 100, 62
    arr = new_canvas(W, H)

    def glyph_shadow(x0, y0, x1, y1):
        rect(arr, x0 + 2, y0 + 2, x1 + 2, y1 + 2, SHADOW_E1)

    def glyph(x0, y0, x1, y1):
        glyph_shadow(x0, y0, x1, y1)
        rect(arr, x0, y0, x1, y1, INK_E1)

    # H: two stems + crossbar
    glyph(4, 6, 12, 54)
    glyph(34, 6, 42, 54)
    glyph(4, 26, 42, 34)
    # i: dot + stem
    glyph(52, 6, 62, 16)
    glyph(52, 24, 62, 54)
    # !: stem + dot
    glyph(72, 6, 82, 40)
    glyph(72, 48, 82, 58)

    img = to_image(arr)
    img.save(f"{OUT}/grid_e1.png")
    return compose_on_bg(
        img, subject_scale=4, bg_color=hexc(BG_E1), canvas=512, radius_ratio=0.20
    )


# ============================================================
# E2: corrected signpost, generous bg margin, MC-native "!" notice glyph
# ============================================================

BG_E2 = "#8FA85C"

PAL_E2 = dict(
    board=hexc("#B8905A"),
    board_hi=hexc("#D2AE78"),
    board_lo=hexc("#8F6B3E"),
    board_edge=hexc("#5B4023"),
    post=hexc("#6B4423"),
    post_hi=hexc("#82552C"),
    post_lo=hexc("#4A2E17"),
    tag=hexc("#F2E7C9"),
    tag_lo=hexc("#D8C79E"),
    mark=hexc("#3A2513"),
)


def build_e2() -> tuple[Image.Image, tuple[int, int, int]]:
    W, H = 64, 68
    arr = new_canvas(W, H)
    pal = PAL_E2

    # post (below board)
    rect_jitter(arr, 27, 40, 37, 66, pal["post"], 5, "e2_post")
    rect(arr, 27, 40, 30, 66, pal["post_hi"])
    rect(arr, 34, 40, 37, 66, pal["post_lo"])

    # board with 1px dark edge + jittered fill + hi/lo bands (hard edges, no AA)
    rect(arr, 2, 4, 62, 44, pal["board_edge"])
    rect_jitter(arr, 4, 6, 60, 42, pal["board"], 6, "e2_board")
    rect(arr, 4, 6, 60, 10, pal["board_hi"])
    rect(arr, 4, 38, 60, 42, pal["board_lo"])
    # grain dashes (observed: vanilla oak_sign.png uses small scattered dark
    # dashes, not plank seams, to texture the board -- ref-checked before draw)
    for gy in (14, 20, 32):
        for gx in range(7, 57, 8):
            d = det_jitter(gx, gy, "e2_grain", 1)
            rect(arr, gx + d, gy, gx + d + 3, gy + 1, pal["board_lo"])

    # notice tag (small lighter plate, nailed-on look) + "!" mark
    rect(arr, 22, 12, 42, 36, pal["tag_lo"])
    rect(arr, 23, 13, 41, 35, pal["tag"])
    rect(arr, 30, 16, 34, 27, pal["mark"])
    rect(arr, 30, 29, 34, 33, pal["mark"])

    img = to_image(arr)
    img.save(f"{OUT}/grid_e2.png")
    return compose_on_bg(
        img, subject_scale=6, bg_color=hexc(BG_E2), canvas=512, radius_ratio=0.20
    )


# ============================================================
# E3: door ajar with warm light spill + mat (re-executed pixel-art, not v1's flat vector)
# ============================================================

BG_E3 = "#3B4A66"

PAL_E3 = dict(
    frame=hexc("#2A1B10"),
    frame_hi=hexc("#4A3018"),
    door=hexc("#7A4B24"),
    door_lo=hexc("#5C3818"),
    light=hexc("#F6D77A"),
    light_lo=hexc("#EFC24E"),
    mat=hexc("#8A5A2E"),
    mat_tick=hexc("#C99A55"),
    knob=hexc("#3A2513"),
)


def build_e3() -> tuple[Image.Image, tuple[int, int, int]]:
    W, H = 64, 66
    arr = new_canvas(W, H)
    pal = PAL_E3

    # door frame (fixed, left side)
    rect(arr, 6, 4, 46, 54, pal["frame"])
    rect(arr, 8, 6, 44, 52, pal["frame_hi"])

    # warm light spilling from the open gap (trapezoid via stepped rows, no AA diagonal)
    steps = [
        (30, 44, 6, 10),
        (30, 46, 10, 14),
        (30, 48, 14, 18),
        (30, 50, 18, 22),
        (30, 52, 22, 26),
        (30, 54, 26, 30),
        (30, 56, 30, 34),
        (30, 58, 34, 38),
        (30, 60, 38, 42),
        (30, 62, 42, 46),
    ]
    for x0, x1, y0, y1 in steps:
        rect(arr, x0, y0, x1, y1, pal["light_lo"])
    for x0, x1, y0, y1 in steps:
        rect(arr, x0, y0, x1 - 2, y0 + 1, pal["light"])

    # door leaf, ajar, swung toward viewer (left edge anchored at hinge x=8)
    door_steps = [
        (8, 30, 8, 10),
        (8, 30, 10, 14),
        (8, 29, 14, 18),
        (8, 28, 18, 22),
        (8, 27, 22, 26),
        (8, 26, 26, 30),
        (8, 25, 30, 34),
        (8, 24, 34, 38),
        (8, 23, 38, 42),
        (8, 22, 42, 46),
        (8, 21, 46, 50),
    ]
    for x0, x1, y0, y1 in door_steps:
        rect(arr, x0, y0, x1, y1, pal["door"])
    rect(arr, 8, 8, 12, 50, pal["door_lo"])
    # knob
    rect(arr, 18, 26, 21, 29, pal["knob"])

    # welcome mat at the threshold
    rect(arr, 4, 56, 48, 62, pal["mat"])
    for tx in range(6, 46, 6):
        rect(arr, tx, 57, tx + 3, 61, pal["mat_tick"])

    img = to_image(arr)
    img.save(f"{OUT}/grid_e3.png")
    return compose_on_bg(
        img, subject_scale=6, bg_color=hexc(BG_E3), canvas=512, radius_ratio=0.20
    )


if __name__ == "__main__":
    import os

    os.makedirs(OUT, exist_ok=True)

    e1, dims1 = build_e1()
    e1.save(f"{FINAL}/welcome_board_E1_hi_text.png")
    report_occupancy("E1 hi_text", *dims1)

    e2, dims2 = build_e2()
    e2.save(f"{FINAL}/welcome_board_E2_signpost_notice.png")
    report_occupancy("E2 signpost_notice", *dims2)

    e3, dims3 = build_e3()
    e3.save(f"{FINAL}/welcome_board_E3_door_ajar.png")
    report_occupancy("E3 door_ajar", *dims3)

    print("done")
