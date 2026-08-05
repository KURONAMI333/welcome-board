# -*- coding: utf-8 -*-
"""Welcome Board icon C-direction pixel-art variants.
Grid pixel art -> NEAREST upscale -> compose onto flat rounded-square bg (Subject-Max).
No smooth gradients / no AA on the subject. Hard discrete tone bands only.
"""

import numpy as np
from PIL import Image, ImageDraw
import hashlib

OUT = "icon-candidates/_pixel_render_work"  # working dir for grid dumps; final PNGs are copied to icon-candidates/ manually
FINAL = "C:/Users/naoki/dev/projects/minecraft-mod-dev/mod-062-welcome-board/branding/icon-candidates"


def hexc(h):
    h = h.lstrip("#")
    return (int(h[0:2], 16), int(h[2:4], 16), int(h[4:6], 16), 255)


def det_jitter(x, y, seed, amp):
    """deterministic pseudo-random jitter in [-amp, amp], no randomness across runs."""
    key = f"{seed}_{x}_{y}".encode()
    v = int(hashlib.md5(key).hexdigest()[:8], 16)
    return (v % (2 * amp + 1)) - amp


def clamp(v):
    return max(0, min(255, v))


def shade(color, delta):
    r, g, b, a = color
    return (clamp(r + delta), clamp(g + delta), clamp(b + delta), a)


# ---------- shared dialog-content painter (rect based, hard edges) ----------


def paint_dialog(arr, ox, oy, w, h, palette, seed):
    """Paint a dialog panel (bevel frame + dark inset + header/lines/button) into arr
    at origin (ox,oy) with size (w,h). palette = dict of named colors.
    arr is HxWx4 uint8 numpy array (mutated in place)."""

    def rect(x0, y0, x1, y1, color):
        arr[oy + y0 : oy + y1, ox + x0 : ox + x1] = color

    def rect_jitter(x0, y0, x1, y1, base, amp, jseed):
        for yy in range(y0, y1):
            for xx in range(x0, x1):
                d = det_jitter(xx, yy, jseed, amp)
                arr[oy + yy, ox + xx] = shade(base, d)

    frame = palette["frame"]
    frame_hi = palette["frame_hi"]
    frame_lo = palette["frame_lo"]
    frame_edge = palette["frame_edge"]
    inset = palette["inset"]
    header = palette["header"]
    divider = palette["divider"]
    body = palette["body"]
    btn = palette["btn"]
    btn_hi = palette["btn_hi"]
    btn_lo = palette["btn_lo"]
    label = palette["label"]

    # outer darkest edge (1px) - relational contrast against whatever sits behind
    rect(0, 0, w, h, frame_edge)
    # bevel frame (3px) inside edge
    rect(1, 1, w - 1, h - 1, frame)
    rect(1, 1, w - 1, 2, frame_hi)  # top highlight
    rect(1, 1, 2, h - 1, frame_hi)  # left highlight
    rect(
        1, h - 3, w - 1, h - 1, frame_lo
    )  # bottom shadow (2px, matches vanilla widget/button.png)
    rect(w - 2, 1, w - 1, h - 1, frame_lo)  # right shadow

    # inner dark panel with subtle grain jitter
    ix0, iy0, ix1, iy1 = 4, 4, w - 4, h - 4
    inset_amp = palette.get("inset_jitter_amp", 4)
    rect_jitter(ix0, iy0, ix1, iy1, inset, inset_amp, seed + "_inset")

    # optional discrete plank/band columns (real oak_planks has ~7 distinct bands,
    # not a single continuous jitter cloud) - only used when palette provides offsets
    plank_offsets = palette.get("plank_col_offsets")
    if plank_offsets:
        n = len(plank_offsets)
        col_w = max(1, (ix1 - ix0) // n)
        for i, off in enumerate(plank_offsets):
            cx0 = ix0 + i * col_w
            cx1 = ix1 if i == n - 1 else cx0 + col_w
            for yy in range(iy0, iy1):
                for xx in range(cx0, cx1):
                    r, g, b, a = (int(v) for v in arr[oy + yy, ox + xx])
                    arr[oy + yy, ox + xx] = shade((r, g, b, a), off)

    # header bar
    hx0, hx1 = ix0 + 3, ix1 - 6
    hy0, hy1 = iy0 + 3, iy0 + 8
    rect(hx0, hy0, hx1, hy1, header)

    # divider 1
    dy = hy1 + 2
    rect(hx0, dy, hx1, dy + 1, divider)

    # body line 1 (longer)
    by0 = dy + 4
    rect(hx0, by0, hx0 + int((hx1 - hx0) * 0.85), by0 + 2, body)
    # body line 2 (shorter)
    by1 = by0 + 5
    rect(hx0, by1, hx0 + int((hx1 - hx0) * 0.65), by1 + 2, body)

    # divider 2
    dy2 = by1 + 8
    if dy2 + 1 < iy1 - 14:
        rect(hx0, dy2, hx1, dy2 + 1, divider)

    # button (bottom, bevel)
    bw = int((ix1 - ix0) * 0.55)
    bh = max(8, int((iy1 - iy0) * 0.16))
    bx0 = ix0 + ((ix1 - ix0) - bw) // 2
    by0b = iy1 - bh - 3
    rect(bx0, by0b, bx0 + bw, by0b + bh, btn)
    rect(bx0, by0b, bx0 + bw, by0b + 2, btn_hi)
    rect(bx0, by0b + bh - 2, bx0 + bw, by0b + bh, btn_lo)
    # label bar centered in button
    lw = int(bw * 0.45)
    lx0 = bx0 + (bw - lw) // 2
    ly0 = by0b + bh // 2 - 1
    rect(lx0, ly0, lx0 + lw, ly0 + 2, label)


def new_canvas(w, h):
    return np.zeros((h, w, 4), dtype=np.uint8)


def to_image(arr):
    return Image.fromarray(arr, "RGBA")


def compose_on_bg(
    subject_img,
    subject_scale,
    bg_color,
    canvas=512,
    radius_ratio=0.20,
    transparent_bg=False,
):
    sw, sh = subject_img.size
    disp_w, disp_h = sw * subject_scale, sh * subject_scale
    subj_big = subject_img.resize((disp_w, disp_h), Image.NEAREST)

    base = Image.new("RGBA", (canvas, canvas), (0, 0, 0, 0))
    if not transparent_bg:
        r = int(canvas * radius_ratio)
        mask = Image.new("L", (canvas, canvas), 0)
        d = ImageDraw.Draw(mask)
        d.rounded_rectangle([0, 0, canvas - 1, canvas - 1], radius=r, fill=255)
        bgfill = Image.new("RGBA", (canvas, canvas), bg_color)
        base = Image.composite(bgfill, base, mask)

    px = (canvas - disp_w) // 2
    py = (canvas - disp_h) // 2
    base.alpha_composite(subj_big, (px, py))
    return base


# ============================================================
# Variant 1: vanilla GUI stone/chrome panel
# ============================================================


def build_v1():
    w, h = 48, 54
    arr = new_canvas(w, h)
    pal = dict(
        frame=hexc("#6f6f6f"),
        frame_hi=hexc("#aaaaaa"),
        frame_lo=hexc("#565656"),
        frame_edge=hexc("#000000"),
        inset=hexc("#1c1c20"),
        header=hexc("#ededed"),
        divider=hexc("#57575c"),
        body=hexc("#b9b9bd"),
        btn=hexc("#6f6f6f"),
        btn_hi=hexc("#aaaaaa"),
        btn_lo=hexc("#565656"),
        label=hexc("#e8e0c8"),
    )
    paint_dialog(arr, 0, 0, w, h, pal, seed="v1")
    img = to_image(arr)
    img.save(f"{OUT}/grid_v1.png")
    bg = hexc(
        "#5f7a9e"
    )  # muted world-sky blue sampled from actual screenshot (~#6d7d99)
    return compose_on_bg(img, 9, bg)


# ============================================================
# Variant 2: wood board (pinned notices)
# ============================================================


def build_v2():
    w, h = 48, 54
    arr = new_canvas(w, h)
    pal = dict(
        frame=hexc("#4f3218"),
        frame_hi=hexc("#6b4620"),
        frame_lo=hexc("#33200f"),
        frame_edge=hexc("#241609"),
        inset=hexc("#b8945f"),  # oak plank board surface
        header=hexc("#fdf6e5"),  # parchment cream note
        divider=hexc("#8a6a3d"),
        body=hexc("#f3e6c8"),
        btn=hexc("#4f3218"),
        btn_hi=hexc("#6b4620"),
        btn_lo=hexc("#2f1c0d"),
        label=hexc("#fdf6e5"),
        inset_jitter_amp=9,  # block-tier grain (CRAFT_TEXTURE_RULES: block jitter 8-10)
        plank_col_offsets=[
            10,
            -8,
            4,
            -12,
            8,
            -4,
        ],  # discrete plank bands, oak_planks-style
    )
    paint_dialog(arr, 0, 0, w, h, pal, seed="v2")

    # wood grain streaks on the board surface (jitter already applied inside paint_dialog's
    # rect_jitter for the inset region -- add a few darker plank-seam rows for readability)
    for gy in (18, 34):
        for xx in range(5, w - 5):
            if (xx // 6) % 2 == 0:
                arr[gy, xx] = shade(pal["inset"], -18)

    # pin dots at the corner of header (red) and body1 (green) - reuse the KURONAMI
    # pinboard-anytime accent-dot motif (small colored dot on a paper note), not copied verbatim
    arr[7, 8] = hexc("#a8402c")
    arr[7, 9] = hexc("#a8402c")
    arr[8, 8] = hexc("#a8402c")
    arr[17, 8] = hexc("#4c7a3d")
    arr[17, 9] = hexc("#4c7a3d")
    arr[18, 8] = hexc("#4c7a3d")

    img = to_image(arr)
    img.save(f"{OUT}/grid_v2.png")
    bg = hexc("#5b7a4f")  # muted mossy green backdrop (board mounted outdoors)
    return compose_on_bg(img, 9, bg)


# ============================================================
# Variant 3: stone tablet block (isometric extrusion)
# ============================================================


def build_v3():
    fw, fh = 40, 46  # front face size
    dx, dy = 10, 8  # depth vector (up-right)
    cw, ch = fw + dx + 2, fh + dy + 2
    arr = new_canvas(cw, ch)

    top_c = hexc("#b7b7b2")
    side_c = hexc("#6f6e6a")
    front_c = hexc("#525250")
    edge_c = hexc("#2c2c2b")

    fx0, fy0 = 1, dy + 1
    fx1, fy1 = fx0 + fw, fy0 + fh

    img = Image.new("RGBA", (cw, ch), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)

    # top face (parallelogram)
    top_poly = [(fx0, fy0), (fx1, fy0), (fx1 + dx, fy0 - dy), (fx0 + dx, fy0 - dy)]
    d.polygon(top_poly, fill=top_c)
    # right side face (parallelogram)
    side_poly = [(fx1, fy0), (fx1, fy1), (fx1 + dx, fy1 - dy), (fx1 + dx, fy0 - dy)]
    d.polygon(side_poly, fill=side_c)
    # front face (rectangle)
    d.rectangle([fx0, fy0, fx1, fy1], fill=front_c)
    # outline edges (relational dark, thin)
    d.line([top_poly[0], top_poly[1]], fill=edge_c)
    d.line([top_poly[1], top_poly[2]], fill=edge_c)
    d.line([top_poly[2], top_poly[3]], fill=edge_c)
    d.line([top_poly[3], top_poly[0]], fill=edge_c)
    d.line([side_poly[0], side_poly[1]], fill=edge_c)
    d.line([side_poly[1], side_poly[2]], fill=edge_c)
    d.line([side_poly[2], side_poly[3]], fill=edge_c)
    d.rectangle([fx0, fy0, fx1, fy1], outline=edge_c)

    arr = np.array(img)

    # speckle jitter on top/side/front to read as stone (grain), deterministic
    for yy in range(ch):
        for xx in range(cw):
            r, g, b, a = (int(v) for v in arr[yy, xx])
            if a == 0:
                continue
            if (r, g, b, a) in (top_c, side_c, front_c):
                amp = 6
                jd = det_jitter(xx, yy, "v3stone", amp)
                arr[yy, xx] = shade((r, g, b, a), jd)

    # engraved dialog content on front face (smaller inset)
    pal = dict(
        frame=hexc("#3d3d3c"),
        frame_hi=hexc("#585856"),
        frame_lo=hexc("#232322"),
        frame_edge=hexc("#171716"),
        inset=hexc("#26262a"),
        header=hexc("#d9d9d9"),
        divider=hexc("#4a4a4c"),
        body=hexc("#9d9da0"),
        btn=hexc("#57575a"),
        btn_hi=hexc("#7a7a7d"),
        btn_lo=hexc("#303032"),
        label=hexc("#d9d9d9"),
    )
    margin = 4
    paint_dialog(
        arr,
        fx0 + margin,
        fy0 + margin,
        fw - 2 * margin,
        fh - 2 * margin,
        pal,
        seed="v3",
    )

    img2 = to_image(arr)
    # transparent background (Subject-Max option (a)) — the extruded block reads as a
    # standalone object, matching how vanilla item icons are usually presented
    return compose_on_bg(img2, 8, (0, 0, 0, 0), transparent_bg=True)


if __name__ == "__main__":
    import os

    os.makedirs(OUT, exist_ok=True)
    v1 = build_v1()
    v1.save(f"{OUT}/v1_vanilla_gui.png")
    v2 = build_v2()
    v2.save(f"{OUT}/v2_woodboard.png")
    v3 = build_v3()
    v3.save(f"{OUT}/v3_isoblock.png")
    print("done")
