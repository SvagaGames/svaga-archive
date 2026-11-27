"""Puts raw phone screenshots into store frames: python3 frame.py ru shot1.png shot2.png ...
Captions come from CAPTIONS in the same order. Output: framed_<lang>_<n>.png, 1080x1920."""
import sys
from PIL import Image, ImageDraw, ImageFont, ImageFilter
F = '/usr/share/fonts/opentype/inter/'
CAPTIONS = {
    'ru': [('Вся ретро-библиотека', 'NES, SNES, SEGA, GBA и ещё сотни платформ'),
           ('Отмечай, во что играешь', 'Играю, пройдено, отложено, оценки и заметки'),
           ('Викторина дня', 'Угадай игру по скриншоту'),
           ('Достижения и уровни', 'Сотни наград, сезоны и тематические испытания'),
           ('Подборки и друзья', 'Делись списками игр и смотри библиотеки друзей'),
           ('Коллекция на полке', 'Картриджи, коробки и инструкции'),
           ('Во что поиграть?', 'Колесо по твоему бэклогу'),
           ('RetroAchievements', 'Подключи свой профиль')],
    'en': [('Your whole retro library', 'NES, SNES, SEGA, GBA and hundreds more'),
           ('Track what you play', 'Playing, completed, backlog, ratings and notes'),
           ('Daily quiz', 'Guess the game by a screenshot'),
           ('Achievements and levels', 'Hundreds of rewards, seasons and themed challenges'),
           ('Lists and friends', 'Share game lists and see friends\' libraries'),
           ('Collection on the shelf', 'Carts, boxes and manuals'),
           ('What to play?', 'Spin the wheel over your backlog'),
           ('RetroAchievements', 'Connect your profile')],
}
W, H = 1080, 1920
def bg():
    g = Image.new('RGB', (W, H))
    d = ImageDraw.Draw(g)
    for y in range(H):
        t = y / H
        d.line([(0, y), (W, y)], fill=(int(20 - 10 * t), int(30 - 16 * t), int(52 - 34 * t)))
    glow = Image.new('L', (W, H), 0)
    ImageDraw.Draw(glow).ellipse([-200, -300, W + 200, 700], fill=90)
    glow = glow.filter(ImageFilter.GaussianBlur(200))
    return Image.composite(Image.new('RGB', (W, H), (30, 100, 120)), g, glow)

def frame(lang, i, path):
    title, sub = CAPTIONS[lang][i % len(CAPTIONS[lang])]
    im = bg()
    d = ImageDraw.Draw(im)
    d.text((W / 2, 150), title, font=ImageFont.truetype(F + 'Inter-ExtraBold.otf', 66), fill=(243, 245, 249), anchor='mm')
    d.text((W / 2, 235), sub, font=ImageFont.truetype(F + 'Inter-Medium.otf', 36), fill=(154, 163, 181), anchor='mm')
    shot = Image.open(path).convert('RGB')
    sw = 800
    sh = int(shot.height * sw / shot.width)
    maxh = H - 330
    if sh > maxh:
        shot = shot.crop((0, 0, shot.width, int(shot.width * maxh / sw)))
        sh = maxh
    shot = shot.resize((sw, sh), Image.LANCZOS)
    x, y = (W - sw) // 2, 330
    # phone body
    pad = 18
    d.rounded_rectangle([x - pad, y - pad, x + sw + pad, y + sh + pad + 400], radius=70, fill=(28, 32, 41), outline=(62, 224, 255), width=3)
    mask = Image.new('L', (sw, sh), 0)
    ImageDraw.Draw(mask).rounded_rectangle([0, 0, sw, sh + 200], radius=54, fill=255)
    im.paste(shot, (x, y), mask)
    out = f'framed_{lang}_{i + 1}.png'
    im.save(out)
    return out

if __name__ == '__main__':
    lang = sys.argv[1]
    for i, p in enumerate(sys.argv[2:]):
        print(frame(lang, i, p))
