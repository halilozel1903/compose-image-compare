# Draws the demo photos of the compose-image-compare sample app: procedural landscapes made of gradients, fractal
# ridges and simple shapes. They are not photographs of real places. Released under CC0 1.0 (public domain).
#
#   python3 scripts/demo-photos.py sample/src/main/res/drawable-nodpi
import math, random, os, sys
from PIL import Image, ImageDraw, ImageFilter, ImageChops

OUT = sys.argv[1]
os.makedirs(OUT, exist_ok=True)

def lerp(a, b, t): return a + (b - a) * t
def mix(c1, c2, t): return tuple(int(round(lerp(c1[i], c2[i], t))) for i in range(3))
def hexc(h): h = h.lstrip('#'); return tuple(int(h[i:i+2], 16) for i in (0, 2, 4))

def gradient(w, h, stops):
    img = Image.new('RGB', (w, h))
    d = ImageDraw.Draw(img)
    stops = [(p, hexc(c)) for p, c in stops]
    for y in range(h):
        t = y / (h - 1)
        for i in range(len(stops) - 1):
            p0, c0 = stops[i]; p1, c1 = stops[i + 1]
            if p0 <= t <= p1:
                c = mix(c0, c1, (t - p0) / max(1e-6, p1 - p0)); break
        else:
            c = stops[-1][1]
        d.line([(0, y), (w, y)], fill=c)
    return img

def glow(img, cx, cy, r, color, strength=0.9, spread=4.0):
    w, h = img.size
    layer = Image.new('RGB', (w, h), (0, 0, 0))
    d = ImageDraw.Draw(layer)
    steps = 40
    for i in range(steps, 0, -1):
        rr = r * spread * i / steps
        a = (1 - i / steps) ** 2.2 * strength
        c = tuple(int(v * a) for v in hexc(color))
        d.ellipse([cx - rr, cy - rr, cx + rr, cy + rr], fill=c)
    layer = layer.filter(ImageFilter.GaussianBlur(r * 0.6))
    return ImageChops.add(img, layer)

def disc(img, cx, cy, r, color):
    d = ImageDraw.Draw(img)
    d.ellipse([cx - r, cy - r, cx + r, cy + r], fill=hexc(color))

def ridge(w, base, amp, rough, seed, peaks=None):
    rnd = random.Random(seed)
    n = 257
    pts = [0.0] * n
    pts[0] = rnd.uniform(-1, 1); pts[-1] = rnd.uniform(-1, 1)
    step = n - 1; scale = 1.0
    while step > 1:
        half = step // 2
        for i in range(half, n, step):
            pts[i] = (pts[i - half] + pts[i + half]) / 2 + rnd.uniform(-1, 1) * scale
        step = half; scale *= rough
    if peaks:
        for (px, ph, pw) in peaks:
            for i in range(n):
                x = i / (n - 1)
                pts[i] += ph * max(0.0, 1 - abs(x - px) / pw) ** 1.4
    lo, hi = min(pts), max(pts)
    return [(i / (n - 1) * w, base - (p - lo) / (hi - lo + 1e-9) * amp) for i, p in enumerate(pts)]

def fill_ridge(img, pts, color, h=None):
    w, hh = img.size
    d = ImageDraw.Draw(img)
    d.polygon(pts + [(w, hh), (0, hh)], fill=hexc(color) if isinstance(color, str) else color)

def snow_caps(img, pts, line, color='#f4f6fb'):
    d = ImageDraw.Draw(img)
    for i in range(len(pts) - 1):
        (x0, y0), (x1, y1) = pts[i], pts[i + 1]
        if y0 < line and y1 < line:
            d.polygon([(x0, y0), (x1, y1), (x1, min(line, y1 + (line - y1) * 0.5)), (x0, min(line, y0 + (line - y0) * 0.5))], fill=hexc(color))

def fog(img, y, height, color, alpha):
    w, h = img.size
    layer = Image.new('RGB', (w, h), hexc(color))
    mask = Image.new('L', (w, h), 0)
    d = ImageDraw.Draw(mask)
    d.rectangle([0, y - height / 2, w, y + height / 2], fill=int(255 * alpha))
    mask = mask.filter(ImageFilter.GaussianBlur(height * 0.45))
    return Image.composite(layer, img, mask)

def stars(img, count, maxy, seed):
    rnd = random.Random(seed)
    d = ImageDraw.Draw(img)
    w, _ = img.size
    for _ in range(count):
        x, y = rnd.uniform(0, w), rnd.uniform(0, maxy) ** 1.0
        b = rnd.randint(150, 255)
        r = rnd.choice([0.8, 1.0, 1.2, 1.6, 2.2])
        d.ellipse([x - r, y - r, x + r, y + r], fill=(b, b, min(255, b + 10)))

def pines(img, base_pts, seed, color, count, hmin, hmax, xrange=(0, 1)):
    rnd = random.Random(seed)
    d = ImageDraw.Draw(img)
    w, _ = img.size
    for _ in range(count):
        x = rnd.uniform(xrange[0], xrange[1]) * w
        i = min(len(base_pts) - 1, int(x / w * (len(base_pts) - 1)))
        y = base_pts[i][1] + rnd.uniform(4, 30)
        th = rnd.uniform(hmin, hmax); tw = th * 0.32
        d.polygon([(x, y - th), (x - tw, y), (x + tw, y)], fill=hexc(color))
        d.rectangle([x - tw * 0.08, y, x + tw * 0.08, y + th * 0.12], fill=hexc(color))

def birds(img, x, y, n, seed, color='#2a2233', size=10):
    rnd = random.Random(seed)
    d = ImageDraw.Draw(img)
    for _ in range(n):
        bx, by = x + rnd.uniform(-80, 80), y + rnd.uniform(-40, 40)
        s = size * rnd.uniform(0.6, 1.2)
        d.line([(bx - s, by - s * 0.4), (bx, by), (bx + s, by - s * 0.4)], fill=hexc(color), width=max(2, int(s / 4)))

def cabin(img, cx, base, size, wall='#4a2e2a', roof='#2b1d22', light='#ffd27a'):
    d = ImageDraw.Draw(img)
    w = size; h = size * 0.62
    d.rectangle([cx - w / 2, base - h, cx + w / 2, base], fill=hexc(wall))
    d.polygon([(cx - w * 0.62, base - h), (cx, base - h - size * 0.55), (cx + w * 0.62, base - h)], fill=hexc(roof))
    d.rectangle([cx + w * 0.18, base - h - size * 0.5, cx + w * 0.3, base - h - size * 0.18], fill=hexc(roof))
    win = size * 0.2
    d.rectangle([cx - w * 0.32, base - h * 0.7, cx - w * 0.32 + win, base - h * 0.7 + win], fill=hexc(light))
    d.rectangle([cx + w * 0.08, base - h * 0.62, cx + w * 0.08 + win * 0.9, base], fill=hexc('#2a1a18'))
    # smoke
    for i in range(6):
        r = size * (0.05 + i * 0.03)
        sx = cx + w * 0.24 + math.sin(i) * size * 0.08 + i * size * 0.05
        sy = base - h - size * 0.55 - i * size * 0.16
        d.ellipse([sx - r, sy - r, sx + r, sy + r], fill=mix(hexc('#f3d6dc'), hexc('#ffffff'), i / 6))
    return img

def reflect(img, horizon, water_color, alpha=0.55, ripple_seed=1):
    w, h = img.size
    top = img.crop((0, 0, w, horizon))
    refl = top.transpose(Image.FLIP_TOP_BOTTOM)
    refl = refl.resize((w, h - horizon)) if refl.size[1] != h - horizon else refl
    refl = refl.filter(ImageFilter.GaussianBlur(3))
    water = Image.new('RGB', refl.size, hexc(water_color))
    refl = Image.blend(refl, water, alpha)
    d = ImageDraw.Draw(refl)
    rnd = random.Random(ripple_seed)
    for _ in range(int(w * 0.35)):
        y = rnd.uniform(0, refl.size[1]) ; x = rnd.uniform(0, w); l = rnd.uniform(10, 90)
        c = refl.getpixel((int(min(w - 1, x)), int(min(refl.size[1] - 1, y))))
        c = tuple(min(255, v + 18) for v in c)
        d.line([(x, y), (x + l, y)], fill=c, width=2)
    img.paste(refl, (0, horizon))
    return img

def grain(img, seed, amount=4):
    # A little film grain so the demo pictures have texture like a photo; filters and zoom show more with it.
    rnd = random.Random(seed)
    w, h = img.size
    noise = Image.effect_noise((w, h), 40).convert('L')
    noise = noise.point(lambda v: 128 + (v - 128) * amount // 40)
    return ImageChops.add(img, Image.merge('RGB', (noise, noise, noise)), scale=1.0, offset=-128)

def finish(img, name):
    img = img.filter(ImageFilter.GaussianBlur(0.6))
    img = grain(img, hash(name) & 0xffff)
    img.save(os.path.join(OUT, name + '.jpg'), quality=84, optimize=True, progressive=True)


w, h = 1600, 1200

# 1. Mirror lake: snowy peaks over a lake with pines and their reflection.
img = gradient(w, h, [(0, '#3f86d1'), (0.45, '#9cc9ee'), (0.6, '#dcecf7'), (1, '#dcecf7')])
img = glow(img, w * 0.78, h * 0.16, 50, '#fff6dc', 0.5)
disc(img, w * 0.78, h * 0.16, 40, '#fffaf0')
hz = int(h * 0.6)
r1 = ridge(w, hz - 40, h * 0.36, 0.55, 21, peaks=[(0.4, 1.6, 0.18), (0.7, 1.0, 0.22)])
fill_ridge(img, r1, '#6f86a8'); snow_caps(img, r1, hz - h * 0.25)
r2 = ridge(w, hz, h * 0.14, 0.5, 22)
fill_ridge(img, r2, '#3e5b5a')
pines(img, r2, 23, '#24403a', 70, 40, 90)
img = reflect(img, hz, '#2f5f7f', 0.45, 24)
finish(img, 'mirror_lake')

# 2. Quiet fjord with a red sailing boat in the middle (the zoomed screenshot looks at it).
img = gradient(w, h, [(0, '#5d7f9e'), (0.55, '#c3d3df'), (1, '#c3d3df')])
hz = int(h * 0.62)
d = ImageDraw.Draw(img)
left = ridge(w * 0.45, hz, h * 0.5, 0.55, 121, peaks=[(0.2, 1.2, 0.4)])
d.polygon(left + [(w * 0.56, hz), (0, hz)], fill=hexc('#3e4f5d'))
right = [(w - x, y) for x, y in ridge(w * 0.5, hz, h * 0.55, 0.55, 122, peaks=[(0.25, 1.4, 0.4)])]
d.polygon(right + [(w * 0.4, hz), (w, hz)], fill=hexc('#34444f'))
img = fog(img, hz - 30, 70, '#dbe5ec', 0.5)
mid = ridge(w, hz, h * 0.08, 0.5, 123); fill_ridge(img, mid, '#596d7b')
pines(img, mid, 125, '#2f3f3a', 26, 30, 60, (0.0, 0.3))
pines(img, mid, 126, '#2f3f3a', 22, 30, 60, (0.72, 1.0))
img = reflect(img, hz, '#41596a', 0.45, 124)
d = ImageDraw.Draw(img)
bx, by = w * 0.5, hz + 110
d.polygon([(bx - 70, by), (bx + 70, by), (bx + 52, by + 22), (bx - 56, by + 22)], fill=hexc('#b5332b'))
d.rectangle([bx - 70, by - 4, bx + 70, by], fill=hexc('#f0e6d8'))
d.line([(bx, by), (bx, by - 120)], fill=hexc('#2a2a2a'), width=5)
d.polygon([(bx + 4, by - 116), (bx + 66, by - 10), (bx + 4, by - 10)], fill=hexc('#f4efe6'))
d.polygon([(bx - 4, by - 100), (bx - 50, by - 10), (bx - 4, by - 10)], fill=hexc('#e2b04a'))
d.polygon([(bx - 2, by - 120), (bx + 24, by - 113), (bx - 2, by - 106)], fill=hexc('#2b5bd7'))
for i in range(7):
    yy = by + 30 + i * 9
    d.line([(bx - 90 + i * 6, yy), (bx + 90 - i * 6, yy)], fill=hexc('#8a3b36') if i % 2 == 0 else hexc('#6e8fa6'), width=2)
birds(img, w * 0.3, h * 0.2, 4, 127, '#2a3440', 12)
finish(img, 'quiet_fjord')

# 3. Autumn hills: layered hills covered in round orange and red trees.
img = gradient(w, h, [(0, '#7fb3d9'), (0.55, '#d8e6ef'), (1, '#efe3cf')])
for i, (c, s) in enumerate([('#c9925f', 101), ('#c46a32', 102), ('#a9471f', 103), ('#7b2f1a', 104)]):
    r = ridge(w, h * (0.5 + i * 0.13), h * 0.13, 0.5, s); fill_ridge(img, r, c)
    rnd = random.Random(s)
    dd = ImageDraw.Draw(img)
    for _ in range(90):
        x = rnd.uniform(0, w); k = min(len(r) - 1, int(x / w * (len(r) - 1))); y = r[k][1] + rnd.uniform(5, 50)
        rr = rnd.uniform(10, 26) * (1 + i * 0.3)
        dd.ellipse([x - rr, y - rr, x + rr, y + rr], fill=mix(hexc(rnd.choice(['#e8a33d', '#d9622b', '#f2c14e', '#b8401f', '#6f8f3a'])), hexc(c), 0.2))
birds(img, w * 0.66, h * 0.18, 5, 105, '#3a3040', 12)
finish(img, 'autumn_hills')
print('done')
