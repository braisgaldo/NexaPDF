#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Escribe las notas de versión de Google Play en los trece idiomas.

Hasta ahora se escribían a mano, un fichero por idioma, y el límite de 500
caracteres de Play se descubría al pegarlas en el navegador. Aquí el texto está
en un solo sitio, el script **falla** si a algún idioma le falta la nota o si se
pasa del límite, y de paso genera el `TODAS.txt` con las etiquetas de locale que
Play Console acepta pegar de una vez.

Uso:
    python tools/generar_notas_version.py             # genera
    python tools/generar_notas_version.py --comprobar # solo valida
"""

from __future__ import annotations

import argparse
import pathlib
import sys

sys.path.insert(0, str(pathlib.Path(__file__).resolve().parent))

from ficha_tienda import IDIOMAS, LOCALES  # noqa: E402

DESTINO = (
    pathlib.Path(__file__).resolve().parent.parent
    / "docs" / "google_play" / "notas-version"
)

# Play corta las notas de versión a 500 caracteres por idioma.
LIMITE = 500

NOTAS = {
    "es": """Novedades

Toca dos veces para acercarte justo donde has tocado, y otras dos para volver a ver la página entera. Funciona leyendo página a página y en continuo.

• Lo que estaba bajo el dedo se queda bajo el dedo, también en las esquinas.
• En lectura continua, ampliar vuelve a funcionar y no se pierde al pasar de página.
• Corregido: el número de página no se actualizaba al volver a la página por la que abriste el documento.""",
    "en": """What's new

Double-tap to zoom in exactly where you tap, and double-tap again to see the whole page. Works both page by page and in continuous scrolling.

• What was under your finger stays under your finger, even in the corners.
• In continuous scrolling, zooming works again and stays as you move on to the next page.
• Fixed: the page number didn't update when going back to the page you opened the document on.""",
    "fr": """Nouveautés

Touchez deux fois pour zoomer exactement là où vous touchez, et encore deux fois pour revoir la page entière. Fonctionne page par page et en défilement continu.

• Ce qui était sous votre doigt reste sous votre doigt, même dans les coins.
• En défilement continu, le zoom fonctionne à nouveau et se conserve d'une page à l'autre.
• Corrigé : le numéro de page ne se mettait pas à jour en revenant à la page d'ouverture.""",
    "de": """Neu

Zweimal tippen vergrößert genau die angetippte Stelle, erneut zweimal tippen zeigt wieder die ganze Seite. Funktioniert seitenweise und beim durchgehenden Scrollen.

• Was unter dem Finger war, bleibt unter dem Finger – auch in den Ecken.
• Beim durchgehenden Scrollen funktioniert das Zoomen wieder und bleibt beim Seitenwechsel erhalten.
• Behoben: Die Seitenzahl wurde bei der Rückkehr zur Anfangsseite nicht aktualisiert.""",
    "zh": """新功能

双击即可放大点按的位置，再次双击即可看到整页。逐页阅读和连续滚动都支持。

• 手指下的内容放大后仍在手指下，角落也一样。
• 连续滚动时缩放恢复正常，翻到下一页也会保持。
• 修复：回到打开文档时所在的页面时，页码没有更新。""",
    "ja": """新機能

ダブルタップでタップした場所をそのまま拡大し、もう一度ダブルタップでページ全体に戻ります。1 ページずつの表示でも連続スクロールでも使えます。

• 指の下にあったものは、拡大しても指の下に。隅でも同じです。
• 連続スクロールでの拡大が正しく動作し、次のページに進んでも保たれます。
• 修正：最初に開いたページに戻ったとき、ページ番号が更新されない問題。""",
    "ru": """Что нового

Двойное касание приближает именно то место, которого вы коснулись, а повторное возвращает всю страницу. Работает и постранично, и при непрерывной прокрутке.

• То, что было под пальцем, остаётся под пальцем — даже в углах.
• При непрерывной прокрутке масштаб снова работает и сохраняется при переходе на следующую страницу.
• Исправлено: номер страницы не обновлялся при возврате на страницу, с которой открыт документ.""",
    "it": """Novità

Tocca due volte per ingrandire proprio dove tocchi, e altre due per rivedere la pagina intera. Funziona pagina per pagina e con lo scorrimento continuo.

• Ciò che era sotto il dito resta sotto il dito, anche negli angoli.
• Con lo scorrimento continuo lo zoom funziona di nuovo e resta passando alla pagina successiva.
• Corretto: il numero di pagina non si aggiornava tornando alla pagina di apertura.""",
    "el": """Τι νέο υπάρχει

Πατήστε δύο φορές για μεγέθυνση ακριβώς εκεί που αγγίζετε και άλλες δύο για να δείτε ξανά όλη τη σελίδα. Λειτουργεί ανά σελίδα και με συνεχή κύλιση.

• Ό,τι ήταν κάτω από το δάχτυλο μένει εκεί, ακόμη και στις γωνίες.
• Στη συνεχή κύλιση η μεγέθυνση λειτουργεί ξανά και διατηρείται στην επόμενη σελίδα.
• Διορθώθηκε: ο αριθμός σελίδας δεν ενημερωνόταν στην επιστροφή στη σελίδα ανοίγματος.""",
    "ar": """الجديد

المس مرتين للتكبير حيث لمست تمامًا، ومرتين أخريين لرؤية الصفحة كاملة. يعمل في القراءة صفحةً صفحة وفي التمرير المتواصل.

• ما كان تحت إصبعك يبقى تحت إصبعك، حتى في الزوايا.
• في التمرير المتواصل يعمل التكبير من جديد ويبقى عند الانتقال إلى الصفحة التالية.
• إصلاح: رقم الصفحة لم يكن يتحدّث عند العودة إلى الصفحة التي فُتح عليها المستند.""",
    "gl": """Novidades

Toca dúas veces para achegarte xusto onde tocaches, e outras dúas para volver ver a páxina enteira. Funciona páxina a páxina e en desprazamento continuo.

• O que estaba baixo o dedo queda baixo o dedo, tamén nas esquinas.
• No desprazamento continuo, ampliar volve funcionar e mantense ao pasar de páxina.
• Corrixido: o número de páxina non se actualizaba ao volver á páxina pola que abriches o documento.""",
    "ca": """Novetats

Toca dues vegades per apropar-te just on has tocat, i dues més per tornar a veure la pàgina sencera. Funciona pàgina a pàgina i amb desplaçament continu.

• El que era sota el dit es queda sota el dit, també a les cantonades.
• Amb desplaçament continu, ampliar torna a funcionar i es manté en passar de pàgina.
• Corregit: el número de pàgina no s'actualitzava en tornar a la pàgina on havies obert el document.""",
    "eu": """Berritasunak

Ukitu birritan ukitu duzun lekura zehazki hurbiltzeko, eta beste birritan orri osoa berriro ikusteko. Orriz orri eta etengabeko korritzean dabil.

• Hatzaren azpian zegoena hatzaren azpian geratzen da, baita ertzetan ere.
• Etengabeko korritzean, handitzeak berriro funtzionatzen du eta orriz aldatzean mantentzen da.
• Konponduta: orri-zenbakia ez zen eguneratzen dokumentua ireki zenuen orrira itzultzean.""",
}


def validar() -> list[str]:
    problemas = []
    faltan = set(IDIOMAS) - set(NOTAS)
    sobran = set(NOTAS) - set(IDIOMAS)
    if faltan:
        problemas.append(f"faltan idiomas: {sorted(faltan)}")
    if sobran:
        problemas.append(f"idiomas desconocidos: {sorted(sobran)}")
    for idioma, texto in sorted(NOTAS.items()):
        if len(texto) > LIMITE:
            problemas.append(f"{idioma}: {len(texto)} caracteres, el limite es {LIMITE}")
        if not texto.strip():
            problemas.append(f"{idioma}: texto vacio")
    return problemas


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--comprobar", action="store_true")
    args = parser.parse_args()

    problemas = validar()
    if problemas:
        print("Las notas de version tienen problemas:", file=sys.stderr)
        for problema in problemas:
            print("  - " + problema, file=sys.stderr)
        return 1

    print(f"{len(NOTAS)} idiomas, todos dentro de los {LIMITE} caracteres de Play.")
    for idioma in IDIOMAS:
        print(f"   {LOCALES[idioma]:>7}  {len(NOTAS[idioma])}/{LIMITE}")

    if args.comprobar:
        return 0

    DESTINO.mkdir(parents=True, exist_ok=True)
    juntas = []
    for idioma in IDIOMAS:
        locale = LOCALES[idioma]
        (DESTINO / f"{locale}.txt").write_text(NOTAS[idioma] + "\n", encoding="utf-8")
        juntas.append(f"<{locale}>\n{NOTAS[idioma]}\n</{locale}>")
    (DESTINO / "TODAS.txt").write_text("\n".join(juntas) + "\n", encoding="utf-8")

    print(f"\nEscrito en {DESTINO}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
