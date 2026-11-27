from PIL import Image, ImageDraw, ImageFont, ImageFilter
import sys
ICON = '/home/claude/svaga-archive/docs/icon.png'
F = '/usr/share/fonts/opentype/inter/'
def font(w, s): return ImageFont.truetype(F + {'b': 'Inter-Bold.otf', 'x': 'Inter-ExtraBold.otf', 'm': 'Inter-Medium.otf', 's': 'Inter-SemiBold.otf'}[w], s)

def bg(w, h):
    im = Image.new('RGB', (w, h), (10, 12, 17))
    g = Image.new('RGB', (w, h))
    d = ImageDraw.Draw(g)
    for y in range(h):
        t = y / h
        d.line([(0, y), (w, y)], fill=(int(18 - 8 * t), int(26 - 12 * t), int(46 - 26 * t)))
    im = g
    glow = Image.new('L', (w, h), 0)
    ImageDraw.Draw(glow).ellipse([int(w * 0.02), int(-h * 0.3), int(w * 0.55), int(h * 1.3)], fill=110)
    glow = glow.filter(ImageFilter.GaussianBlur(h // 4))
    cyan = Image.new('RGB', (w, h), (30, 90, 110))
    im = Image.composite(cyan, im, glow)
    return im

def chip(d, x, y, text, f, color):
    tw = d.textlength(text, font=f)
    h = int(f.size * 1.8)
    pad = int(f.size * 0.9)
    d.rounded_rectangle([x, y, x + tw + 2 * pad, y + h], radius=h // 2, outline=color, width=max(2, f.size // 9), fill=(20, 26, 38))
    d.text((x + pad, y + h / 2), text, font=f, fill=color, anchor='lm')
    return x + tw + 2 * pad + int(f.size * 0.7)

def banner(w, h, lang, out):
    im = bg(w, h)
    icon = Image.open(ICON).convert('RGBA')
    size = int(h * 0.62)
    icon = icon.resize((size, size), Image.LANCZOS)
    mask = Image.new('L', (size, size), 0)
    ImageDraw.Draw(mask).rounded_rectangle([0, 0, size, size], radius=size // 5, fill=255)
    im.paste(icon, (int(h * 0.12), (h - size) // 2), mask)
    d = ImageDraw.Draw(im)
    x = int(h * 0.12) + size + int(h * 0.09)
    k = h / 500
    d.text((x, int(118 * k)), 'SVAGA', font=font('x', int(52 * k)), fill=(62, 224, 255))
    sw = d.textlength('SVAGA ', font=font('x', int(52 * k)))
    d.text((x + sw, int(118 * k)), 'ARCHIVE', font=font('x', int(52 * k)), fill=(243, 245, 249))
    tag = 'Твоя библиотека ретро-игр' if lang == 'ru' else 'Your retro game library'
    d.text((x, int(190 * k)), tag, font=font('s', int(30 * k)), fill=(220, 226, 236))
    sub = 'Коллекция, достижения и друзья' if lang == 'ru' else 'Collection, achievements and friends'
    d.text((x, int(234 * k)), sub, font=font('m', int(22 * k)), fill=(154, 163, 181))
    chips = ['🎮 NES · SNES · SEGA · GBA', ] if False else (['Викторина дня', 'Подборки', 'RetroAchievements'] if lang == 'ru' else ['Daily quiz', 'Game lists', 'RetroAchievements'])
    cx = x
    for c, col in zip(chips, [(185, 139, 255), (255, 181, 71), (61, 220, 132)]):
        f = font('s', int(17 * k))
        if cx + d.textlength(c, font=f) + 2 * f.size > w - int(h * 0.06): break
        cx = chip(d, cx, int(300 * k), c, f, col)
    im.save(out, quality=95)

banner(1024, 500, 'ru', 'feature_1024x500_ru.png')
banner(1024, 500, 'en', 'feature_1024x500_en.png')
banner(1920, 1080, 'ru', 'cover_1920x1080_ru.png')
ic = Image.open(ICON).convert('RGB')
ic.resize((512, 512), Image.LANCZOS).save('icon_512.png')
ic.resize((216, 216), Image.LANCZOS).save('icon_216.png')
print('ok')
