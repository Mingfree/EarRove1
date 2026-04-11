"""Generate adaptive launcher foreground mipmaps and Play Store 512 icon from source PNG."""
from __future__ import annotations

from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parents[1]
SRC = ROOT / "branding" / "ic_launcher_source.png"
RES = ROOT / "app" / "src" / "main" / "res"
GRAPHICS = ROOT / "graphics"

# Pixels with all channels below this are treated as background (transparent in foreground layer).
BLACK_THRESHOLD = 42

DENSITIES = {
    "mipmap-mdpi": 108,
    "mipmap-hdpi": 162,
    "mipmap-xhdpi": 216,
    "mipmap-xxhdpi": 324,
    "mipmap-xxxhdpi": 432,
}


def make_foreground_rgba(src: Image.Image) -> Image.Image:
    src = src.convert("RGBA")
    pixels = src.load()
    w, h = src.size
    thr = BLACK_THRESHOLD
    for y in range(h):
        for x in range(w):
            r, g, b, a = pixels[x, y]
            if r <= thr and g <= thr and b <= thr:
                pixels[x, y] = (0, 0, 0, 0)
    return src


def main() -> None:
    if not SRC.is_file():
        raise SystemExit(f"Source PNG not found: {SRC}")

    original = Image.open(SRC).convert("RGB")
    fg = make_foreground_rgba(original.convert("RGBA"))

    for folder, size in DENSITIES.items():
        out_dir = RES / folder
        out_dir.mkdir(parents=True, exist_ok=True)
        resized = fg.resize((size, size), Image.Resampling.LANCZOS)
        out_path = out_dir / "ic_launcher_foreground.png"
        resized.save(out_path, format="PNG", optimize=True)
        print("Wrote", out_path)

    GRAPHICS.mkdir(parents=True, exist_ok=True)
    play = original.resize((512, 512), Image.Resampling.LANCZOS)
    play_path = GRAPHICS / "ic_launcher_play_512.png"
    play.save(play_path, format="PNG", optimize=True)
    print("Wrote", play_path)


if __name__ == "__main__":
    main()
