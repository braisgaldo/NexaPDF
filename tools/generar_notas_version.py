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

Escáner de documentos. Fotografía un papel y sale un PDF derecho y legible, no una foto torcida dentro de un PDF.

• Encuentra los bordes mientras apuntas y dispara solo cuando la mano está quieta.
• Corrige la perspectiva y hace varias fotos de cada hoja para que salga nítida.
• Modo tarjeta: DNI, carné o pasaporte salen a tamaño real, las dos caras en una hoja.
• Puede leer el texto para que busques dentro. Todo en tu teléfono.
• Guarda con el nombre que elijas.""",
    "en": """What's new

Document scanner. Photograph a sheet of paper and get a straight, readable PDF instead of a crooked photo inside a PDF.

• Finds the edges while you aim and shoots once your hand is still.
• Corrects the perspective and takes several shots per page for a sharp result.
• Card mode: ID, licence or passport come out at actual size, both sides on one sheet.
• Reads the text so you can search inside, all on your phone.
• Saves with your chosen name.""",
    "fr": """Nouveautés

Scanner de documents. Photographiez une feuille et obtenez un PDF droit et lisible, pas une photo de travers dans un PDF.

• Trouve les bords pendant que vous visez et déclenche quand la main est immobile.
• Redresse la perspective et nettoie la page.
• Prend plusieurs photos par page et les fusionne : moins de grain.
• Mode carte : pièce d'identité ou passeport à taille réelle, les deux faces sur une feuille.
• Peut lire le texte pour chercher dedans. Tout sur votre téléphone.""",
    "de": """Neu

Dokumentenscanner. Fotografieren Sie ein Blatt und erhalten Sie ein gerades, lesbares PDF statt eines schiefen Fotos.

• Findet die Ränder beim Zielen und löst aus, sobald die Hand ruhig ist.
• Korrigiert die Perspektive und säubert die Seite.
• Macht mehrere Aufnahmen pro Seite und fügt sie zusammen: weniger Rauschen.
• Kartenmodus: Ausweis oder Reisepass in Originalgröße, beide Seiten auf einem Blatt.
• Kann den Text lesen, damit Sie darin suchen können. Alles auf dem Gerät.""",
    "zh": """新功能

文档扫描。拍一张纸，得到的是端正易读的 PDF，而不是一张歪斜的照片。

• 取景时就找到纸张边缘，并显示识别程度。
• 取景合适、手持稳定时自动拍摄。
• 自动校正透视并清理页面。
• 每页拍摄多张并合成：噪点更少，更清晰。
• 证卡模式：身份证或护照按实际尺寸输出，正反面同页。
• 可识别文字，方便在文档里搜索。全部在手机上完成。
• 用你自己起的名字保存。""",
    "ja": """新機能

書類スキャナー。紙を撮るだけで、傾いた写真ではなく、まっすぐで読みやすい PDF になります。

• 構えている間に用紙の輪郭を見つけ、どれだけ見えているかを表示。
• 構図が決まり手が止まったら自動で撮影。
• 傾きを補正してページをきれいに。
• 1 ページにつき複数枚を撮って合成。ノイズが減り、くっきりします。
• カードモード：身分証やパスポートを実物大で、表裏を 1 枚に。
• 文字を読み取れば書類の中を検索できます。すべて端末内で処理。
• 好きな名前を付けて保存。""",
    "ru": """Что нового

Сканер документов. Сфотографируйте лист и получите ровный, читаемый PDF, а не кривое фото внутри PDF.

• Находит края, пока вы наводите, и снимает сам, когда рука замерла.
• Выправляет перспективу и чистит страницу.
• Делает несколько снимков на страницу и объединяет их: меньше шума.
• Режим карточки: удостоверение или паспорт в натуральную величину, обе стороны на листе.
• Может распознать текст, чтобы искать внутри. Всё в телефоне.""",
    "it": """Novità

Scanner di documenti. Fotografa un foglio e ottieni un PDF dritto e leggibile, non una foto storta dentro un PDF.

• Trova i bordi mentre inquadri e scatta da solo quando la mano è ferma.
• Raddrizza la prospettiva e pulisce la pagina.
• Scatta più foto per pagina e le fonde: meno grana, più nitidezza.
• Modalità tessera: documento o passaporto a grandezza reale, fronte e retro in un foglio.
• Può leggere il testo per cercarci dentro. Tutto sul telefono.""",
    "el": """Τι νέο υπάρχει

Σαρωτής εγγράφων. Φωτογραφίστε ένα χαρτί και πάρτε ένα ίσιο, ευανάγνωστο PDF.

• Βρίσκει τις άκρες καθώς στοχεύετε και τραβά μόνο του όταν σταθεροποιηθεί.
• Ισιώνει την προοπτική και καθαρίζει τη σελίδα.
• Τραβά πολλές λήψεις ανά σελίδα και τις συνθέτει: λιγότερος θόρυβος.
• Λειτουργία κάρτας: ταυτότητα ή διαβατήριο σε φυσικό μέγεθος, και οι δύο όψεις σε ένα φύλλο.
• Μπορεί να διαβάσει το κείμενο για αναζήτηση. Όλα στο τηλέφωνο.""",
    "ar": """الجديد

ماسح ضوئي للمستندات. صوّر ورقة واحصل على PDF مستقيم وواضح بدل صورة مائلة.

• يجد حواف الورقة أثناء التصويب ويعرض مدى وضوحها.
• يلتقط تلقائيًا عندما يستقر الإطار وتثبت اليد.
• يصحّح المنظور وينظّف الصفحة.
• يلتقط عدة صور لكل صفحة ويدمجها: ضوضاء أقل ووضوح أعلى.
• وضع البطاقة: الهوية أو جواز السفر بالحجم الحقيقي، والوجهان في ورقة واحدة.
• يمكنه قراءة النص للبحث داخل المستند. كل ذلك داخل هاتفك.""",
    "gl": """Novidades

Dixitalizador de documentos. Fotografía un papel e sae un PDF dereito e lexible, non unha foto torta dentro dun PDF.

• Atopa os bordos mentres apuntas e dispara só cando a man está quieta.
• Corrixe a perspectiva e limpa a páxina.
• Fai varias fotos por páxina e fúndeas: menos gran, máis nitidez.
• Modo tarxeta: DNI ou pasaporte saen a tamaño real, as dúas caras nunha folla.
• Pode ler o texto para buscar dentro. Todo no teu teléfono.""",
    "ca": """Novetats

Escàner de documents. Fotografia un paper i obtens un PDF dret i llegible, no una foto torta dins d'un PDF.

• Troba les vores mentre apuntes i dispara sol quan la mà està quieta.
• Corregeix la perspectiva i neteja la pàgina.
• Fa diverses fotos per pàgina i les fusiona: menys gra, més nitidesa.
• Mode targeta: DNI o passaport a mida real, les dues cares en un full.
• Pot llegir el text per cercar-hi dins. Tot al teu telèfon.""",
    "eu": """Berritasunak

Dokumentu-eskanerra. Atera paper bati argazkia eta PDF zuzen eta irakurgarria lortuko duzu, ez argazki oker bat.

• Ertzak aurkitzen ditu apuntatu ahala eta berak ateratzen du eskua geldi dagoenean.
• Perspektiba zuzentzen du eta orria garbitzen.
• Orriko hainbat argazki ateratzen ditu eta batu: pikor gutxiago.
• Txartel modua: NAN edo pasaportea tamaina errealean, bi aldeak orri berean.
• Testua irakur dezake barruan bilatzeko. Dena zure telefonoan.""",
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
