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

# Adaptive icon keyline: circular mask uses a 72dp circle (36dp radius) inside 108dp layers.
# Scale artwork to fit inside that circle with a small inset so brackets survive circle/squircle crops.
SAFE_RADIUS_FRAC = 36 / 108
SAFE_INSET = 0.85

# Corner “viewfinder” strokes are much darker than the eye/pin; on a black adaptive background
# they read as invisible until we lift luminance (hue preserved via RGB scale).
BRACKET_LIFT_LUMAX = 102
BRACKET_LIFT_FACTOR_CAP = 2.55
# Second pass on each mipmap: downscaling blends dark strokes toward transparency; lift again at output size.
POST_SCALE_LUMAX = 112
POST_SCALE_FACTOR_CAP = 2.4
# Only pixels with all channels below this (dark browns / brackets); avoids flattening main gold.
POST_SCALE_MAX_CHANNEL = 100

# Fraction of width/height defining corner rectangles where the four L-shaped brackets live.
VIEWFINDER_CORNER_FRAC_X = 0.30
VIEWFINDER_CORNER_FRAC_Y = 0.28
# Blend toward this gold so strokes stay readable on adaptive black (still distinct from center motif).
VIEWFINDER_TARGET_RGB = (210, 182, 108)
VIEWFINDER_BLEND = 0.78
# Skip pixels already this bright (center gold bleeding into corners via AA).
VIEWFINDER_SKIP_LUMAX = 168

# If True, corner rectangles are blended toward VIEWFINDER_TARGET_RGB (washes out dark bronze).
# Keep False when the source PNG already has clearly visible L-brackets.
REMAP_VIEWFINDER_CORNERS = False


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


def boost_dark_gold_for_black_bg(
    fg_rgba: Image.Image,
    lumax: float = BRACKET_LIFT_LUMAX,
    factor_cap: float = BRACKET_LIFT_FACTOR_CAP,
    min_alpha: int = 28,
    max_channel: int | None = None,
) -> Image.Image:
    """Raise contrast of very dark gold/brown foreground so thin brackets survive on #000."""
    im = fg_rgba.copy()
    px = im.load()
    w, h = im.size
    for y in range(h):
        for x in range(w):
            r, g, b, a = px[x, y]
            if a <= min_alpha:
                continue
            if max_channel is not None and max(r, g, b) > max_channel:
                continue
            lum = 0.299 * r + 0.587 * g + 0.114 * b
            if lum >= lumax:
                continue
            t = 1.0 - lum / lumax
            factor = 1.0 + t * (factor_cap - 1.0)
            px[x, y] = (
                min(255, int(r * factor)),
                min(255, int(g * factor)),
                min(255, int(b * factor)),
                a,
            )
    return im


def in_viewfinder_corner_zone(x: int, y: int, w: int, h: int) -> bool:
    fx = VIEWFINDER_CORNER_FRAC_X
    fy = VIEWFINDER_CORNER_FRAC_Y
    xl = w * fx
    yl = h * fy
    return (
        (x < xl and y < yl)
        or (x >= w - xl and y < yl)
        or (x < xl and y >= h - yl)
        or (x >= w - xl and y >= h - yl)
    )


def force_viewfinder_L_strokes_visible(fg_rgba: Image.Image) -> Image.Image:
    """Pull four-corner L brackets to a lighter gold; they stay too dark on #000 otherwise."""
    im = fg_rgba.copy()
    px = im.load()
    w, h = im.size
    tr, tg, tb = VIEWFINDER_TARGET_RGB
    blend = VIEWFINDER_BLEND
    skip = VIEWFINDER_SKIP_LUMAX
    omb = 1.0 - blend
    for y in range(h):
        for x in range(w):
            if not in_viewfinder_corner_zone(x, y, w, h):
                continue
            r, g, b, a = px[x, y]
            if a < 24:
                continue
            lum = 0.299 * r + 0.587 * g + 0.114 * b
            if lum >= skip:
                continue
            nr = min(255, int(r * omb + tr * blend))
            ng = min(255, int(g * omb + tg * blend))
            nb = min(255, int(b * omb + tb * blend))
            px[x, y] = (nr, ng, nb, a)
    return im


def alpha_bbox(img: Image.Image) -> tuple[int, int, int, int]:
    """Return (left, top, right, bottom) inclusive bounds of pixels with alpha > 0."""
    a = img.split()[-1]
    return a.getbbox() or (0, 0, img.width - 1, img.height - 1)


def compose_foreground_for_adaptive(fg_rgba: Image.Image, out_size: int) -> Image.Image:
    """Center the logo inside out_size square, scaled to fit the adaptive-icon safe circle."""
    left, top, right, bottom = alpha_bbox(fg_rgba)
    cropped = fg_rgba.crop((left, top, right + 1, bottom + 1))
    if REMAP_VIEWFINDER_CORNERS:
        cropped = force_viewfinder_L_strokes_visible(cropped)
    w, h = cropped.size
    half_diag = (w * w / 4 + h * h / 4) ** 0.5
    max_r = out_size * SAFE_RADIUS_FRAC * SAFE_INSET
    scale = max_r / half_diag if half_diag > 0 else 1.0
    new_w = max(1, int(round(w * scale)))
    new_h = max(1, int(round(h * scale)))
    scaled = cropped.resize((new_w, new_h), Image.Resampling.LANCZOS)
    canvas = Image.new("RGBA", (out_size, out_size), (0, 0, 0, 0))
    paste_x = (out_size - new_w) // 2
    paste_y = (out_size - new_h) // 2
    canvas.paste(scaled, (paste_x, paste_y), scaled)
    return canvas


def main() -> None:
    if not SRC.is_file():
        raise SystemExit(f"Source PNG not found: {SRC}")

    original = Image.open(SRC).convert("RGB")
    fg = make_foreground_rgba(original.convert("RGBA"))
    fg = boost_dark_gold_for_black_bg(fg)

    for folder, size in DENSITIES.items():
        out_dir = RES / folder
        out_dir.mkdir(parents=True, exist_ok=True)
        resized = compose_foreground_for_adaptive(fg, size)
        resized = boost_dark_gold_for_black_bg(
            resized,
            lumax=POST_SCALE_LUMAX,
            factor_cap=POST_SCALE_FACTOR_CAP,
            min_alpha=18,
            max_channel=POST_SCALE_MAX_CHANNEL,
        )
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
