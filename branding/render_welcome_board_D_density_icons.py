# -*- coding: utf-8 -*-
"""Welcome Board icon D-density variants (C1 vanilla_gui direction, less density).

kura instruction (2026-08-04): keep the C1 register (vanilla GUI bevel frame,
measured palette, pixel grid) exactly as-is, but reduce the number of interior
elements and give them real whitespace so the icon reads at 48px. Three density
steps (D1 > D2 > D3) are produced so kura can pick the level.

Frame/bevel/palette are byte-identical in construction to
render_welcome_board_C_pixel_icons.py::build_v1 (same 48x54 grid, same
subject_scale=9, same bg color) so the register is unchanged -- only the
interior content layout differs.
"""

import numpy as np
from PIL import Image
import hashlib

OUT = "icon-candidates/_pixel_render_work"
FINAL = "C:/Users/naoki/dev/projects/minecraft-mod-dev/mod-062-welcome-board/branding/icon-candidates"

W, H = 48, 54  # same total grid as C1 -- register unchanged
SUBJECT_SCALE = 9  # same as C1
BG = "#5f7a9e"  # same bg as C1


def hexc(h):
    h = h.lstrip("#")
    return (int(h[0:2], 16), int(h[2:4], 16), int(h[4:6], 16), 255)


def det_jitter(x, y, seed, amp):
    key = f"{seed}_{x}_{y}".encode()
    v = int(hashlib.md5(key).hexdigest()[:8], 16)
    return (v % (2 * amp + 1)) - amp


def clamp(v):
    return max(0, min(255, v))


def shade(color, delta):
    r, g, b, a = color
    return (clamp(r + delta), clamp(g + delta), clamp(b + delta), a)


PAL = dict(
    frame=hexc("#6f6f6f"),
    frame_hi=hexc("#aaaaaa"),
    frame_lo=hexc("#565656"),
    frame_edge=hexc("#000000"),
    inset=hexc("#1c1c20"),
    header=hexc("#ededed"),
    body=hexc("#b9b9bd"),
    btn=hexc("#6f6f6f"),
    btn_hi=hexc("#aaaaaa"),
    btn_lo=hexc("#565656"),
    label=hexc("#e8e0c8"),
)


def new_canvas(w, h):
    return np.zeros((h, w, 4), dtype=np.uint8)


def to_image(arr):
    return Image.fromarray(arr, "RGBA")


def paint_frame(arr, w, h, pal, seed):
    """Identical construction to paint_dialog's frame+inset section in the C script."""

    def rect(x0, y0, x1, y1, color):
        arr[y0:y1, x0:x1] = color

    def rect_jitter(x0, y0, x1, y1, base, amp, jseed):
        for yy in range(y0, y1):
            for xx in range(x0, x1):
                d = det_jitter(xx, yy, jseed, amp)
                arr[yy, xx] = shade(base, d)

    rect(0, 0, w, h, pal["frame_edge"])
    rect(1, 1, w - 1, h - 1, pal["frame"])
    rect(1, 1, w - 1, 2, pal["frame_hi"])
    rect(1, 1, 2, h - 1, pal["frame_hi"])
    rect(1, h - 3, w - 1, h - 1, pal["frame_lo"])
    rect(w - 2, 1, w - 1, h - 1, pal["frame_lo"])

    ix0, iy0, ix1, iy1 = 4, 4, w - 4, h - 4
    rect_jitter(ix0, iy0, ix1, iy1, pal["inset"], 4, seed + "_inset")
    return ix0, iy0, ix1, iy1


def draw_bar(arr, x0, y0, x1, y1, color):
    arr[y0:y1, x0:x1] = color


def draw_button(arr, x0, y0, x1, y1, pal, label_w_frac=0.45, label_h=2):
    bh = y1 - y0
    arr[y0:y1, x0:x1] = pal["btn"]
    arr[y0 : y0 + 2, x0:x1] = pal["btn_hi"]
    arr[y1 - 2 : y1, x0:x1] = pal["btn_lo"]
    bw = x1 - x0
    lw = int(bw * label_w_frac)
    lx0 = x0 + (bw - lw) // 2
    ly0 = y0 + bh // 2 - label_h // 2
    arr[ly0 : ly0 + label_h, lx0 : lx0 + lw] = pal["label"]


def compose_on_bg(subject_img, subject_scale, bg_color, canvas=512, radius_ratio=0.20):
    from PIL import ImageDraw

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
    return base


# ============================================================
# D1: header + body1 + body2 + button (dividers dropped)
# ============================================================


def build_d1():
    arr = new_canvas(W, H)
    ix0, iy0, ix1, iy1 = paint_frame(arr, W, H, PAL, seed="d1")
    pad_x, pad_y = 6, 6
    cx0, cx1 = ix0 + pad_x, ix1 - pad_x  # 10..38 (w=28)
    y = iy0 + pad_y  # 10

    header_t, gap1 = 5, 5
    body1_t, gap2 = 3, 4
    body2_t, gap3 = 3, 6
    btn_t = 8

    draw_bar(arr, cx0, y, cx1, y + header_t, PAL["header"])
    y += header_t + gap1
    draw_bar(arr, cx0, y, cx1, y + body1_t, PAL["body"])
    y += body1_t + gap2
    body2_w = cx0 + int((cx1 - cx0) * 0.70)
    draw_bar(arr, cx0, y, body2_w, y + body2_t, PAL["body"])
    y += body2_t + gap3
    bw = int((cx1 - cx0) * 0.60)
    bx0 = cx0 + ((cx1 - cx0) - bw) // 2
    draw_button(arr, bx0, y, bx0 + bw, y + btn_t, PAL)
    y += btn_t

    img = to_image(arr)
    img.save(f"{OUT}/grid_d1.png")
    return compose_on_bg(img, SUBJECT_SCALE, hexc(BG)), {
        "header": header_t,
        "gap1": gap1,
        "body1": body1_t,
        "gap2": gap2,
        "body2": body2_t,
        "gap3": gap3,
        "button": btn_t,
        "pad_x": pad_x,
        "pad_y": pad_y,
    }


# ============================================================
# D2: header + single body line + button
# ============================================================


def build_d2():
    arr = new_canvas(W, H)
    ix0, iy0, ix1, iy1 = paint_frame(arr, W, H, PAL, seed="d2")
    pad_x, pad_y = 6, 5
    cx0, cx1 = ix0 + pad_x, ix1 - pad_x  # 10..38 (w=28)
    y = iy0 + pad_y  # 9

    header_t, gap1 = 6, 6
    body_t, gap2 = 5, 9
    btn_t = 10

    draw_bar(arr, cx0, y, cx1, y + header_t, PAL["header"])
    y += header_t + gap1
    body_w = cx0 + int((cx1 - cx0) * 0.85)
    draw_bar(arr, cx0, y, body_w, y + body_t, PAL["body"])
    y += body_t + gap2
    bw = int((cx1 - cx0) * 0.68)
    bx0 = cx0 + ((cx1 - cx0) - bw) // 2
    draw_button(arr, bx0, y, bx0 + bw, y + btn_t, PAL, label_h=2)
    y += btn_t

    img = to_image(arr)
    img.save(f"{OUT}/grid_d2.png")
    return compose_on_bg(img, SUBJECT_SCALE, hexc(BG)), {
        "header": header_t,
        "gap1": gap1,
        "body": body_t,
        "gap2": gap2,
        "button": btn_t,
        "pad_x": pad_x,
        "pad_y": pad_y,
    }


# ============================================================
# D3: header + button only (no body text at all)
# ============================================================


def build_d3():
    arr = new_canvas(W, H)
    ix0, iy0, ix1, iy1 = paint_frame(arr, W, H, PAL, seed="d3")
    pad_x, pad_y = 5, 5
    cx0, cx1 = ix0 + pad_x, ix1 - pad_x  # 9..39 (w=30)
    y = iy0 + pad_y  # 9

    header_t, gap1 = 8, 14
    btn_t = 14

    draw_bar(arr, cx0, y, cx1, y + header_t, PAL["header"])
    y += header_t + gap1
    bw = int((cx1 - cx0) * 0.75)
    bx0 = cx0 + ((cx1 - cx0) - bw) // 2
    draw_button(arr, bx0, y, bx0 + bw, y + btn_t, PAL, label_h=3)
    y += btn_t

    img = to_image(arr)
    img.save(f"{OUT}/grid_d3.png")
    return compose_on_bg(img, SUBJECT_SCALE, hexc(BG)), {
        "header": header_t,
        "gap1": gap1,
        "button": btn_t,
        "pad_x": pad_x,
        "pad_y": pad_y,
    }


if __name__ == "__main__":
    import os

    os.makedirs(OUT, exist_ok=True)

    d1, spec1 = build_d1()
    d1.save(f"{FINAL}/welcome_board_D1_density.png")
    d2, spec2 = build_d2()
    d2.save(f"{FINAL}/welcome_board_D2_density.png")
    d3, spec3 = build_d3()
    d3.save(f"{FINAL}/welcome_board_D3_density.png")

    print("D1", spec1)
    print("D2", spec2)
    print("D3", spec3)
    print("done")
