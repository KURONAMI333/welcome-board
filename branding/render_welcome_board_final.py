# -*- coding: utf-8 -*-
"""Welcome Board icon finalization -- confirmed candidate E1 (hi_text).

kura confirmed E1 (welcome_board_E1_hi_text.png) as the final icon
(2026-08-05). This script writes the size-variant exports required by
LOGO_PLAYBOOK's finalization gate:

  branding/welcome_board_icon_{512,256,128,64}.png
  common/src/main/resources/welcome_board.png (in-jar, 256px)
  ../mod-063-welcome-board-26x/neoforge-26.2/src/main/resources/welcome_board.png (256px)
  ../mod-063-welcome-board-26x/neoforge-26.1.2/src/main/resources/welcome_board.png (256px)

512 is exactly divisible by 256/128/64 (factors 2/4/8), so all size
variants are produced by integer-factor NEAREST downscale from the
512px source -- no re-render from the native pixel grid is needed
(source is already hard-edge pixel art, no AA to reintroduce).
"""

from pathlib import Path

from PIL import Image

BRANDING = Path(__file__).parent
SOURCE = (
    BRANDING
    / "icon-candidates"
    / "welcome_board_E1_hi_text_CONFIRMED_see_branding_root_for_final.png"
)

MOD062_ROOT = BRANDING.parent
MOD063_ROOT = MOD062_ROOT.parent / "mod-063-welcome-board-26x"

SIZES = [512, 256, 128, 64]

IN_JAR_TARGETS = [
    MOD062_ROOT / "common" / "src" / "main" / "resources" / "welcome_board.png",
    MOD063_ROOT / "neoforge-26.2" / "src" / "main" / "resources" / "welcome_board.png",
    MOD063_ROOT
    / "neoforge-26.1.2"
    / "src"
    / "main"
    / "resources"
    / "welcome_board.png",
]
IN_JAR_SIZE = 256


def main() -> None:
    src = Image.open(SOURCE).convert("RGBA")
    assert src.size == (512, 512), f"unexpected source size {src.size}"

    variants: dict[int, Image.Image] = {}
    for size in SIZES:
        if size == 512:
            img = src
        else:
            factor = 512 // size
            assert 512 % size == 0, f"{size} does not divide 512 evenly"
            img = src.resize((size, size), Image.NEAREST)
        variants[size] = img
        out_path = BRANDING / f"welcome_board_icon_{size}.png"
        img.save(out_path)
        print(f"wrote {out_path} ({img.size[0]}x{img.size[1]})")

    in_jar_img = variants[IN_JAR_SIZE]
    for target in IN_JAR_TARGETS:
        target.parent.mkdir(parents=True, exist_ok=True)
        in_jar_img.save(target)
        print(f"wrote {target} ({in_jar_img.size[0]}x{in_jar_img.size[1]})")


if __name__ == "__main__":
    main()
