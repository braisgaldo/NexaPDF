#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Textos de la ficha de Google Play en los trece idiomas.

Google Play limita: titulo a 30 caracteres, descripcion corta a 80 y
descripcion larga a 4000. `generar_ficha.py` comprueba los tres limites y falla
si alguno se pasa, que es mejor que descubrirlo pegando el texto en la consola.

Reglas de redaccion, heredadas del propio proyecto:

  - Prohibidas las palabras comprar, pagar, desbloquear, pro, premium,
    suscripcion y precio. La donacion no se menciona como si fuera una compra.
  - Nada de "version completa" ni de funciones reservadas: no las hay.
  - No se promete fidelidad de conversion que no existe.
"""

IDIOMAS = ["en", "es", "fr", "de", "zh", "ja", "ru", "it", "el", "ar", "gl", "ca", "eu"]

# Carpeta de Play Console para cada idioma.
LOCALES = {
    "en": "en-US",
    "es": "es-ES",
    "fr": "fr-FR",
    "de": "de-DE",
    "zh": "zh-CN",
    "ja": "ja-JP",
    "ru": "ru-RU",
    "it": "it-IT",
    "el": "el-GR",
    "ar": "ar",
    "gl": "gl-ES",
    "ca": "ca",
    "eu": "eu-ES",
}

TITULO = {
    "en": "NexaPDF: PDF tools offline",
    "es": "NexaPDF: PDF sin conexión",
    "fr": "NexaPDF : outils PDF",
    "de": "NexaPDF: PDF ohne Internet",
    "zh": "NexaPDF：离线 PDF 工具",
    "ja": "NexaPDF：オフラインPDF",
    "ru": "NexaPDF: PDF офлайн",
    "it": "NexaPDF: PDF offline",
    "el": "NexaPDF: PDF χωρίς ίντερνετ",
    "ar": "NexaPDF: أدوات PDF",
    "gl": "NexaPDF: PDF sen conexión",
    "ca": "NexaPDF: PDF sense connexió",
    "eu": "NexaPDF: PDF konexiorik gabe",
}

DESCRIPCION_CORTA = {
    "en": "Merge, split, edit and sign PDFs offline. No ads, no tracking, no accounts.",
    "es": "Une, separa, edita y firma PDF sin conexión. Sin anuncios y sin rastreo.",
    "fr": "Fusionnez, divisez, modifiez et signez des PDF. Sans internet, sans publicité.",
    "de": "PDFs zusammenfügen, teilen, bearbeiten, signieren. Ohne Netz, ohne Werbung.",
    "zh": "在手机上合并、拆分、编辑和签署 PDF。无需联网，没有广告，不做追踪。",
    "ja": "PDFの結合・分割・編集・署名をスマホで。ネット接続不要、広告なし、追跡なし。",
    "ru": "Объединяйте, делите, редактируйте и подписывайте PDF. Без интернета и рекламы.",
    "it": "Unisci, dividi, modifica e firma PDF offline. Senza pubblicità né tracciamento.",
    "el": "Ενώστε, χωρίστε, επεξεργαστείτε και υπογράψτε PDF χωρίς ίντερνετ.",
    "ar": "ادمج وقسّم وحرّر ووقّع ملفات PDF على هاتفك. بلا إنترنت وبلا إعلانات وبلا تتبّع.",
    "gl": "Une, separa, edita e asina PDF sen conexión. Sen anuncios e sen rastrexo.",
    "ca": "Uneix, divideix, edita i signa PDF sense connexió. Sense anuncis ni rastreig.",
    "eu": "Batu, zatitu, editatu eta sinatu PDFak. Internetik gabe, iragarkirik gabe.",
}

DESCRIPCION_LARGA = {
    "en": """NexaPDF is a complete PDF toolbox that runs entirely on your phone.

It does not ask for the internet permission. That is not a promise: it is a restriction of the operating system itself, and anyone can check it in the app manifest. Your documents cannot leave your device, because the app has no way to send them anywhere.

WHAT IT DOES

• Merge documents into one, dragging to set the order, with a preview of each first page. It also accepts Word, Excel, PowerPoint and images: anything that is not a PDF is converted first.
• Split a document into one file per page, by ranges, or by extracting the pages you pick.
• Turn photos into documents, from your gallery or straight from the camera. One image per page, or 2, 4 and 6 together.
• Edit pages: draw by hand, highlight, add shapes, arrows, text boxes and images. Replace a line of existing text. Apply page filters to clean up a photographed document.
• Sign by hand, or with your own certificate. A certificate signature is embedded so any PDF reader can check that the document has not changed since you signed it.
• Reorder pages by dragging, and see the result before applying it.
• Convert between PDF and Word, Excel and PowerPoint, in both directions.

PRIVACY, PLAINLY

• No internet permission. No permissions at all, in fact.
• No accounts, no sign-up, no analytics, no crash reporting, no advertising.
• Your certificate is read, used and discarded. It is never stored.
• The working folder is emptied every time the app starts.
• Files you create are saved to Downloads/NexaPDF on your own phone.

HONEST ABOUT CONVERSION

Converting between PDF and Office formats keeps the content, not the exact layout. A PDF stores letters placed on a page, not paragraphs and cells, so the structure is reconstructed as well as it can be. The app tells you this before converting rather than promising something it cannot deliver.

MADE TO BE COMFORTABLE

Six themes, three light and three dark, plus following your system setting. Thirteen languages, with Arabic laid out right to left. Content descriptions for screen readers and touch targets that respect accessibility guidelines.

FREE, AND STAYING THAT WAY

Every feature is available to everyone. There is no locked content, no reserved functions and no second version. If the app is useful to you, there is a voluntary way to say thanks inside settings. Nothing changes: everything stays exactly as available as before.

Source code: github.com/braisgaldo/NexaPDF""",

    "es": """NexaPDF es una caja de herramientas para PDF que funciona entera en tu móvil.

No pide el permiso de internet. Eso no es una promesa: es una restricción del propio sistema operativo, y cualquiera puede comprobarlo en el manifiesto de la aplicación. Tus documentos no pueden salir del dispositivo porque la app no tiene forma de enviarlos a ninguna parte.

QUÉ HACE

• Unir documentos en uno solo, arrastrando para ordenarlos y con la vista previa de la primera página de cada uno. Admite también Word, Excel, PowerPoint e imágenes: lo que no es PDF se convierte antes.
• Separar un documento en un fichero por página, por rangos o extrayendo las páginas que elijas.
• Convertir fotos en documentos, desde la galería o haciendo la foto en el momento. Una imagen por página, o 2, 4 y 6 juntas.
• Editar páginas: dibujar a mano, resaltar, añadir formas, flechas, cajas de texto e imágenes. Sustituir una línea del texto que ya hay. Aplicar filtros para dejar limpio un documento fotografiado.
• Firmar a mano o con tu propio certificado. La firma con certificado queda incrustada para que cualquier lector de PDF pueda comprobar que el documento no ha cambiado desde que lo firmaste.
• Reordenar páginas arrastrando, viendo el resultado antes de aplicarlo.
• Convertir entre PDF y Word, Excel y PowerPoint, en los dos sentidos.

PRIVACIDAD, SIN ADORNOS

• Sin permiso de internet. Sin ningún permiso, de hecho.
• Sin cuentas, sin registro, sin analítica, sin informes de fallos y sin publicidad.
• Tu certificado se lee, se usa y se descarta. No se guarda nunca.
• La carpeta de trabajo se vacía cada vez que arranca la aplicación.
• Los ficheros que creas se guardan en Descargas/NexaPDF, en tu propio teléfono.

SINCERIDAD SOBRE LA CONVERSIÓN

Convertir entre PDF y los formatos de ofimática conserva el contenido, no el diseño exacto. Un PDF guarda letras colocadas sobre una página, no párrafos ni celdas, así que la estructura se reconstruye lo mejor posible. La aplicación te lo dice antes de convertir en vez de prometer algo que no puede cumplir.

PENSADA PARA QUE SE USE A GUSTO

Seis temas, tres claros y tres oscuros, más la opción de seguir al sistema. Trece idiomas, con el árabe de derecha a izquierda. Descripciones para lectores de pantalla y áreas táctiles que respetan las guías de accesibilidad.

GRATIS, Y ASÍ SEGUIRÁ

Todas las funciones están disponibles para todo el mundo. No hay contenido bloqueado, ni funciones reservadas, ni una segunda versión. Si la aplicación te resulta útil, en los ajustes hay una forma voluntaria de dar las gracias. No cambia nada: todo sigue igual de disponible para todo el mundo.

Código fuente: github.com/braisgaldo/NexaPDF""",

    "fr": """NexaPDF est une boîte à outils PDF qui fonctionne entièrement sur votre téléphone.

Elle ne demande pas la permission d'accès à internet. Ce n'est pas une promesse : c'est une restriction du système d'exploitation lui-même, vérifiable par n'importe qui dans le manifeste de l'application. Vos documents ne peuvent pas quitter votre appareil, car l'application n'a aucun moyen de les envoyer où que ce soit.

CE QU'ELLE FAIT

• Fusionner des documents en un seul, en les faisant glisser pour les ordonner, avec un aperçu de la première page de chacun. Elle accepte aussi Word, Excel, PowerPoint et les images : ce qui n'est pas un PDF est converti au préalable.
• Diviser un document en un fichier par page, par plages, ou en extrayant les pages choisies.
• Transformer des photos en documents, depuis la galerie ou en prenant la photo sur le moment. Une image par page, ou 2, 4 et 6 ensemble.
• Modifier les pages : dessiner à main levée, surligner, ajouter des formes, des flèches, des zones de texte et des images. Remplacer une ligne du texte existant. Appliquer des filtres pour nettoyer un document photographié.
• Signer à la main ou avec votre propre certificat. La signature par certificat est intégrée pour que n'importe quel lecteur PDF puisse vérifier que le document n'a pas changé depuis la signature.
• Réordonner les pages en les faisant glisser, en voyant le résultat avant de l'appliquer.
• Convertir entre PDF et Word, Excel et PowerPoint, dans les deux sens.

CONFIDENTIALITÉ, SANS DÉTOUR

• Pas de permission internet. Aucune permission, en réalité.
• Pas de compte, pas d'inscription, pas d'analyse, pas de rapport d'erreur, pas de publicité.
• Votre certificat est lu, utilisé puis abandonné. Il n'est jamais conservé.
• Le dossier de travail est vidé à chaque démarrage de l'application.
• Les fichiers que vous créez sont enregistrés dans Téléchargements/NexaPDF, sur votre propre téléphone.

HONNÊTETÉ SUR LA CONVERSION

La conversion entre PDF et formats bureautiques conserve le contenu, pas la mise en page exacte. Un PDF stocke des lettres placées sur une page, pas des paragraphes ni des cellules : la structure est donc reconstituée au mieux. L'application vous le dit avant de convertir plutôt que de promettre ce qu'elle ne peut pas tenir.

CONÇUE POUR ÊTRE AGRÉABLE

Six thèmes, trois clairs et trois sombres, plus le suivi du réglage du système. Treize langues, avec l'arabe de droite à gauche. Descriptions pour les lecteurs d'écran et zones tactiles conformes aux règles d'accessibilité.

GRATUITE, ET ELLE LE RESTERA

Toutes les fonctions sont accessibles à tout le monde. Aucun contenu verrouillé, aucune fonction réservée, aucune seconde version. Si l'application vous est utile, les réglages proposent une façon volontaire de dire merci. Rien ne change : tout reste aussi accessible qu'avant.

Code source : github.com/braisgaldo/NexaPDF""",

    "de": """NexaPDF ist ein PDF-Werkzeugkasten, der vollständig auf Ihrem Telefon läuft.

Die App fordert keine Internetberechtigung an. Das ist kein Versprechen, sondern eine Einschränkung des Betriebssystems selbst, und jeder kann es im Manifest der App nachprüfen. Ihre Dokumente können das Gerät nicht verlassen, weil die App keine Möglichkeit hat, sie irgendwohin zu senden.

WAS SIE KANN

• Dokumente zu einem zusammenfügen, per Ziehen ordnen, mit Vorschau der ersten Seite jedes Dokuments. Auch Word, Excel, PowerPoint und Bilder werden angenommen: Was kein PDF ist, wird vorher umgewandelt.
• Ein Dokument aufteilen: eine Datei pro Seite, nach Bereichen, oder ausgewählte Seiten entnehmen.
• Fotos in Dokumente verwandeln, aus der Galerie oder direkt mit der Kamera. Ein Bild pro Seite, oder 2, 4 und 6 zusammen.
• Seiten bearbeiten: freihändig zeichnen, markieren, Formen, Pfeile, Textfelder und Bilder hinzufügen. Eine vorhandene Textzeile ersetzen. Filter anwenden, um ein abfotografiertes Dokument aufzuräumen.
• Handschriftlich unterschreiben oder mit dem eigenen Zertifikat. Die Zertifikatsignatur wird eingebettet, sodass jeder PDF-Betrachter prüfen kann, dass sich das Dokument seit der Unterschrift nicht geändert hat.
• Seiten per Ziehen neu ordnen und das Ergebnis vor dem Übernehmen sehen.
• Zwischen PDF und Word, Excel und PowerPoint umwandeln, in beide Richtungen.

DATENSCHUTZ, OHNE UMSCHWEIFE

• Keine Internetberechtigung. Genau genommen überhaupt keine Berechtigungen.
• Keine Konten, keine Registrierung, keine Analyse, keine Absturzberichte, keine Werbung.
• Ihr Zertifikat wird gelesen, verwendet und verworfen. Es wird nie gespeichert.
• Der Arbeitsordner wird bei jedem Start der App geleert.
• Erstellte Dateien landen unter Downloads/NexaPDF auf Ihrem eigenen Telefon.

EHRLICH ZUR UMWANDLUNG

Die Umwandlung zwischen PDF und Office-Formaten erhält den Inhalt, nicht das exakte Layout. Ein PDF speichert Buchstaben auf einer Seite, keine Absätze und Zellen; die Struktur wird also so gut wie möglich rekonstruiert. Die App sagt Ihnen das vor dem Umwandeln, statt etwas zu versprechen, das sie nicht halten kann.

ANGENEHM IM GEBRAUCH

Sechs Designs, drei helle und drei dunkle, dazu die Systemeinstellung. Dreizehn Sprachen, mit Arabisch von rechts nach links. Inhaltsbeschreibungen für Screenreader und Tippflächen nach den Barrierefreiheitsrichtlinien.

KOSTENLOS, UND DAS BLEIBT SO

Alle Funktionen stehen allen zur Verfügung. Keine gesperrten Inhalte, keine reservierten Funktionen, keine zweite Fassung. Wenn Ihnen die App nützt, gibt es in den Einstellungen eine freiwillige Art, Danke zu sagen. Es ändert sich nichts: alles bleibt genauso verfügbar.

Quellcode: github.com/braisgaldo/NexaPDF""",

    "zh": """NexaPDF 是一套完全在手机上运行的 PDF 工具箱。

它不申请联网权限。这不是一句承诺，而是操作系统本身的限制，任何人都可以在应用清单中查证。你的文档无法离开设备，因为这个应用根本没有把它们发送出去的途径。

功能

• 把多个文档合并为一个，拖动排序，并显示每个文档首页的预览。同样支持 Word、Excel、PowerPoint 和图片：非 PDF 的文件会先转换。
• 拆分文档：每页一个文件、按范围拆分，或提取你选中的页面。
• 把照片变成文档，可从相册选取或当场拍照。每页一张图，或 2、4、6 张排在一起。
• 编辑页面：手绘、荧光标注、添加形状、箭头、文本框和图片。替换已有的一行文字。对拍摄的文档应用增强滤镜。
• 手写签名，或使用你自己的证书签名。证书签名会嵌入文档，任何 PDF 阅读器都能验证文档自签署后未被更改。
• 拖动重新排列页面，应用前可先查看效果。
• 在 PDF 与 Word、Excel、PowerPoint 之间双向转换。

隐私，直说

• 没有联网权限。事实上没有申请任何权限。
• 没有账号、没有注册、没有统计分析、没有崩溃上报、没有广告。
• 你的证书只被读取、使用，然后丢弃，绝不保存。
• 每次启动应用时，工作文件夹都会清空。
• 你创建的文件保存在手机的 下载/NexaPDF 目录中。

关于转换，实话实说

PDF 与办公格式之间的转换保留的是内容，而非精确排版。PDF 保存的是页面上字符的位置，而不是段落和单元格，因此结构只能尽力还原。应用会在转换前告知这一点，而不是承诺做不到的事。

用起来舒服

六款主题，三浅三深，另有跟随系统。十三种语言，其中阿拉伯语为从右到左布局。为屏幕阅读器提供了内容描述，触控区域符合无障碍规范。

免费，并将一直免费

所有功能对所有人开放。没有锁定内容，没有保留功能，也没有第二个版本。如果这个应用对你有用，设置里有一个自愿表示感谢的方式。它不会改变任何事情：所有功能依旧对所有人开放。

源代码：github.com/braisgaldo/NexaPDF""",

    "ja": """NexaPDF は、すべて端末内で動作する PDF の道具箱です。

インターネット権限を要求しません。これは約束ではなく OS 自体による制限で、アプリのマニフェストを見れば誰でも確認できます。送信する手段そのものがないため、あなたの書類が端末の外に出ることはありません。

できること

• 複数の文書を 1 つに結合。ドラッグして順序を変えられ、各文書の 1 ページ目のプレビューが出ます。Word・Excel・PowerPoint・画像も受け付け、PDF でないものは先に変換されます。
• 文書の分割：1 ページごとに 1 ファイル、範囲指定、選んだページの取り出し。
• 写真を文書に変換。ギャラリーから選ぶか、その場で撮影できます。1 ページに 1 枚、または 2・4・6 枚まとめて。
• ページの編集：手書き、蛍光ペン、図形、矢印、テキストボックス、画像の追加。既存の文字行の差し替え。撮影した書類をきれいにする補正フィルター。
• 手書き署名、または自分の証明書による署名。証明書の署名は文書に埋め込まれ、署名後に改変されていないことをどの PDF ビューアでも確認できます。
• ドラッグでページを並べ替え、適用前に結果を確認できます。
• PDF と Word・Excel・PowerPoint を双方向に変換。

プライバシーについて、率直に

• インターネット権限なし。そもそも権限を一切要求しません。
• アカウントなし、登録なし、分析なし、クラッシュ報告なし、広告なし。
• 証明書は読み取って使ったら破棄します。保存は一切しません。
• 作業フォルダーはアプリ起動のたびに空にします。
• 作成したファイルは端末の ダウンロード/NexaPDF に保存されます。

変換について、正直に

PDF とオフィス形式の変換で保たれるのは内容であり、レイアウトそのままではありません。PDF はページ上の文字の位置を保存する形式で、段落やセルを保存しているわけではないため、構造は可能な範囲で組み直されます。できないことを約束せず、変換前にその旨をお伝えします。

気持ちよく使えるように

テーマは 6 種類（ライト 3・ダーク 3）に加えて端末設定に合わせる選択肢。13 言語対応で、アラビア語は右から左のレイアウトです。スクリーンリーダー向けの説明と、アクセシビリティ指針に沿ったタップ領域を用意しています。

無料で、これからも

すべての機能を誰もが使えます。ロックされた内容も、限定機能も、別バージョンもありません。役に立ったと感じたら、設定に任意でお礼を伝える方法があります。何も変わりません。すべての機能はこれまでどおり誰でも使えます。

ソースコード：github.com/braisgaldo/NexaPDF""",

    "ru": """NexaPDF — набор инструментов для PDF, работающий целиком на вашем телефоне.

Приложение не запрашивает разрешение на доступ в интернет. Это не обещание, а ограничение самой операционной системы, и любой может проверить это в манифесте приложения. Ваши документы не могут покинуть устройство, потому что у приложения просто нет способа их куда-либо отправить.

ЧТО ОНО УМЕЕТ

• Объединять документы в один, перетаскивая для изменения порядка, с предпросмотром первой страницы каждого. Принимает также Word, Excel, PowerPoint и изображения: всё, что не PDF, сначала преобразуется.
• Делить документ: по одному файлу на страницу, по диапазонам или извлекая выбранные страницы.
• Превращать фотографии в документы — из галереи или снимая на месте. По одному изображению на страницу, либо 2, 4 и 6 вместе.
• Редактировать страницы: рисовать от руки, выделять маркером, добавлять фигуры, стрелки, надписи и изображения. Заменять строку существующего текста. Применять фильтры, чтобы привести в порядок сфотографированный документ.
• Подписывать от руки или собственным сертификатом. Подпись сертификатом встраивается, и любая программа для чтения PDF может проверить, что документ не менялся после подписания.
• Менять порядок страниц перетаскиванием, видя результат до применения.
• Преобразовывать между PDF и Word, Excel и PowerPoint в обе стороны.

О ПРИВАТНОСТИ, БЕЗ ПРИКРАС

• Нет разрешения на интернет. Собственно, нет вообще никаких разрешений.
• Ни аккаунтов, ни регистрации, ни аналитики, ни отчётов о сбоях, ни рекламы.
• Ваш сертификат читается, используется и отбрасывается. Он никогда не сохраняется.
• Рабочая папка очищается при каждом запуске приложения.
• Созданные файлы сохраняются в Загрузки/NexaPDF на вашем телефоне.

ЧЕСТНО О ПРЕОБРАЗОВАНИИ

Преобразование между PDF и офисными форматами сохраняет содержимое, а не точную вёрстку. PDF хранит буквы, размещённые на странице, а не абзацы и ячейки, поэтому структура восстанавливается настолько, насколько это возможно. Приложение говорит об этом до преобразования, а не обещает того, чего не может.

СДЕЛАНО, ЧТОБЫ БЫЛО УДОБНО

Шесть тем: три светлых и три тёмных, плюс следование настройке системы. Тринадцать языков, арабский — справа налево. Описания для программ чтения с экрана и области нажатия по правилам доступности.

БЕСПЛАТНО И ОСТАНЕТСЯ ТАКИМ

Все возможности доступны всем. Нет заблокированного содержимого, нет зарезервированных функций, нет второй версии. Если приложение вам пригодилось, в настройках есть добровольный способ сказать спасибо. Ничего не меняется: всё остаётся так же доступно, как и раньше.

Исходный код: github.com/braisgaldo/NexaPDF""",

    "it": """NexaPDF è una cassetta degli attrezzi per PDF che gira interamente sul telefono.

Non chiede il permesso di accesso a internet. Non è una promessa: è una restrizione del sistema operativo stesso, e chiunque può verificarla nel manifest dell'applicazione. I tuoi documenti non possono lasciare il dispositivo, perché l'app non ha alcun modo di inviarli da nessuna parte.

COSA FA

• Unire documenti in uno solo, trascinandoli per ordinarli, con l'anteprima della prima pagina di ciascuno. Accetta anche Word, Excel, PowerPoint e immagini: ciò che non è PDF viene convertito prima.
• Dividere un documento: un file per pagina, per intervalli, o estraendo le pagine scelte.
• Trasformare foto in documenti, dalla galleria o scattando sul momento. Un'immagine per pagina, oppure 2, 4 e 6 insieme.
• Modificare le pagine: disegnare a mano libera, evidenziare, aggiungere forme, frecce, caselle di testo e immagini. Sostituire una riga del testo esistente. Applicare filtri per ripulire un documento fotografato.
• Firmare a mano o con il proprio certificato. La firma con certificato viene incorporata, così qualsiasi lettore PDF può verificare che il documento non sia cambiato dopo la firma.
• Riordinare le pagine trascinandole, vedendo il risultato prima di applicarlo.
• Convertire tra PDF e Word, Excel e PowerPoint, in entrambe le direzioni.

PRIVACY, SENZA GIRI DI PAROLE

• Nessun permesso di internet. A dire il vero, nessun permesso del tutto.
• Nessun account, nessuna registrazione, nessuna analisi, nessun rapporto di errore, nessuna pubblicità.
• Il tuo certificato viene letto, usato e scartato. Non viene mai conservato.
• La cartella di lavoro si svuota a ogni avvio dell'applicazione.
• I file che crei finiscono in Download/NexaPDF, sul tuo telefono.

ONESTÀ SULLA CONVERSIONE

La conversione tra PDF e formati per ufficio conserva il contenuto, non l'impaginazione esatta. Un PDF conserva lettere collocate su una pagina, non paragrafi e celle, quindi la struttura viene ricostruita al meglio possibile. L'app te lo dice prima di convertire, invece di promettere ciò che non può mantenere.

PENSATA PER STARE COMODI

Sei temi, tre chiari e tre scuri, più l'opzione di seguire il sistema. Tredici lingue, con l'arabo da destra a sinistra. Descrizioni per i lettori di schermo e aree di tocco che rispettano le linee guida di accessibilità.

GRATIS, E COSÌ RESTERÀ

Tutte le funzioni sono disponibili per tutti. Non c'è contenuto bloccato, né funzioni riservate, né una seconda versione. Se l'app ti è utile, nelle impostazioni c'è un modo volontario per dire grazie. Non cambia nulla: tutto resta disponibile esattamente come prima.

Codice sorgente: github.com/braisgaldo/NexaPDF""",

    "el": """Το NexaPDF είναι μια εργαλειοθήκη για PDF που τρέχει εξ ολοκλήρου στο κινητό σας.

Δεν ζητά άδεια πρόσβασης στο διαδίκτυο. Δεν πρόκειται για υπόσχεση: είναι περιορισμός του ίδιου του λειτουργικού συστήματος, και ο καθένας μπορεί να το επαληθεύσει στο manifest της εφαρμογής. Τα έγγραφά σας δεν μπορούν να φύγουν από τη συσκευή, γιατί η εφαρμογή δεν έχει κανέναν τρόπο να τα στείλει πουθενά.

ΤΙ ΚΑΝΕΙ

• Ενώνει έγγραφα σε ένα, με σύρσιμο για την ταξινόμηση και προεπισκόπηση της πρώτης σελίδας του καθενός. Δέχεται επίσης Word, Excel, PowerPoint και εικόνες: ό,τι δεν είναι PDF μετατρέπεται πρώτα.
• Χωρίζει ένα έγγραφο: ένα αρχείο ανά σελίδα, κατά περιοχές, ή εξάγοντας τις σελίδες που επιλέγετε.
• Μετατρέπει φωτογραφίες σε έγγραφα, από τη συλλογή ή τραβώντας τες επιτόπου. Μία εικόνα ανά σελίδα, ή 2, 4 και 6 μαζί.
• Επεξεργάζεται σελίδες: ελεύθερη σχεδίαση, επισήμανση, σχήματα, βέλη, πλαίσια κειμένου και εικόνες. Αντικατάσταση μιας γραμμής του υπάρχοντος κειμένου. Φίλτρα για να καθαρίσετε ένα φωτογραφημένο έγγραφο.
• Υπογραφή με το χέρι ή με το δικό σας πιστοποιητικό. Η υπογραφή με πιστοποιητικό ενσωματώνεται, ώστε οποιοδήποτε πρόγραμμα ανάγνωσης PDF να μπορεί να επαληθεύσει ότι το έγγραφο δεν άλλαξε μετά την υπογραφή.
• Αναδιάταξη σελίδων με σύρσιμο, βλέποντας το αποτέλεσμα πριν το εφαρμόσετε.
• Μετατροπή ανάμεσα σε PDF και Word, Excel και PowerPoint, και προς τις δύο κατευθύνσεις.

ΑΠΟΡΡΗΤΟ, ΧΩΡΙΣ ΠΕΡΙΣΤΡΟΦΕΣ

• Καμία άδεια διαδικτύου. Στην πραγματικότητα, καμία άδεια απολύτως.
• Χωρίς λογαριασμούς, χωρίς εγγραφή, χωρίς αναλυτικά στοιχεία, χωρίς αναφορές σφαλμάτων, χωρίς διαφημίσεις.
• Το πιστοποιητικό σας διαβάζεται, χρησιμοποιείται και απορρίπτεται. Δεν αποθηκεύεται ποτέ.
• Ο φάκελος εργασίας αδειάζει σε κάθε εκκίνηση της εφαρμογής.
• Τα αρχεία που δημιουργείτε αποθηκεύονται στο Λήψεις/NexaPDF, στο δικό σας τηλέφωνο.

ΕΙΛΙΚΡΙΝΕΙΑ ΓΙΑ ΤΗ ΜΕΤΑΤΡΟΠΗ

Η μετατροπή ανάμεσα σε PDF και μορφές γραφείου διατηρεί το περιεχόμενο, όχι την ακριβή διάταξη. Ένα PDF αποθηκεύει γράμματα τοποθετημένα σε μια σελίδα, όχι παραγράφους και κελιά, οπότε η δομή ανακατασκευάζεται όσο καλύτερα γίνεται. Η εφαρμογή σας το λέει πριν τη μετατροπή αντί να υπόσχεται κάτι που δεν μπορεί να τηρήσει.

ΦΤΙΑΓΜΕΝΗ ΓΙΑ ΝΑ ΕΙΝΑΙ ΒΟΛΙΚΗ

Έξι θέματα, τρία ανοιχτά και τρία σκούρα, συν την επιλογή να ακολουθεί το σύστημα. Δεκατρείς γλώσσες, με τα αραβικά από δεξιά προς τα αριστερά. Περιγραφές για αναγνώστες οθόνης και περιοχές αφής σύμφωνες με τις οδηγίες προσβασιμότητας.

ΔΩΡΕΑΝ, ΚΑΙ ΘΑ ΠΑΡΑΜΕΙΝΕΙ

Όλες οι λειτουργίες είναι διαθέσιμες σε όλους. Δεν υπάρχει κλειδωμένο περιεχόμενο, ούτε δεσμευμένες λειτουργίες, ούτε δεύτερη έκδοση. Αν η εφαρμογή σας φανεί χρήσιμη, στις ρυθμίσεις υπάρχει ένας εθελοντικός τρόπος να πείτε ευχαριστώ. Τίποτα δεν αλλάζει: όλα παραμένουν εξίσου διαθέσιμα.

Πηγαίος κώδικας: github.com/braisgaldo/NexaPDF""",

    "ar": """‏NexaPDF صندوق أدوات لملفات PDF يعمل بالكامل داخل هاتفك.

لا يطلب إذن الوصول إلى الإنترنت. وهذا ليس وعدًا، بل قيد يفرضه نظام التشغيل نفسه، ويمكن لأي شخص التحقق منه في ملف بيان التطبيق. لا يمكن لمستنداتك مغادرة جهازك، لأن التطبيق لا يملك أي وسيلة لإرسالها إلى أي مكان.

ما الذي يفعله

• دمج المستندات في مستند واحد، مع السحب لترتيبها ومعاينة الصفحة الأولى من كل منها. ويقبل أيضًا ملفات Word وExcel وPowerPoint والصور: وكل ما ليس PDF يُحوَّل أولًا.
• تقسيم المستند: ملف لكل صفحة، أو حسب نطاقات، أو باستخراج الصفحات التي تختارها.
• تحويل الصور إلى مستندات، من المعرض أو بالتقاطها في اللحظة. صورة واحدة في كل صفحة، أو ٢ و٤ و٦ معًا.
• تحرير الصفحات: الرسم باليد، والتظليل، وإضافة الأشكال والأسهم ومربعات النص والصور. واستبدال سطر من النص الموجود. وتطبيق مرشّحات لتنظيف مستند مُصوَّر.
• التوقيع بخط اليد أو بشهادتك الخاصة. يُضمَّن التوقيع بالشهادة داخل الملف، فيستطيع أي قارئ PDF التحقق من أن المستند لم يتغيّر بعد توقيعه.
• إعادة ترتيب الصفحات بالسحب، مع رؤية النتيجة قبل تطبيقها.
• التحويل بين PDF وWord وExcel وPowerPoint في الاتجاهين.

الخصوصية، بصراحة

• لا إذن للإنترنت. بل لا أذونات على الإطلاق.
• لا حسابات ولا تسجيل ولا تحليلات ولا تقارير أعطال ولا إعلانات.
• تُقرأ شهادتك وتُستخدم ثم تُهمَل. ولا تُحفظ أبدًا.
• يُفرَّغ مجلد العمل في كل مرة يبدأ فيها التطبيق.
• تُحفظ الملفات التي تنشئها في التنزيلات/NexaPDF داخل هاتفك.

صراحة بشأن التحويل

يحافظ التحويل بين PDF وصيغ المكتب على المحتوى، لا على التنسيق الدقيق. فملف PDF يخزّن حروفًا موضوعة على صفحة، لا فقرات وخلايا، ولذلك يُعاد بناء البنية بأفضل صورة ممكنة. ويخبرك التطبيق بذلك قبل التحويل بدل أن يَعِد بما لا يستطيع.

مصمَّم ليكون مريحًا

ست سمات، ثلاث فاتحة وثلاث داكنة، إضافة إلى خيار اتّباع النظام. ثلاث عشرة لغة، والعربية بتخطيط من اليمين إلى اليسار. أوصاف لقارئات الشاشة ومساحات لمس تحترم إرشادات إمكانية الوصول.

مجاني، وسيبقى كذلك

كل الميزات متاحة للجميع. لا محتوى مقفل ولا وظائف محجوزة ولا نسخة ثانية. وإن وجدت التطبيق مفيدًا، ففي الإعدادات طريقة اختيارية لقول شكرًا. ولا يتغيّر شيء: تبقى كل الميزات متاحة للجميع كما هي.

الشيفرة المصدرية: github.com/braisgaldo/NexaPDF""",

    "gl": """NexaPDF é unha caixa de ferramentas para PDF que funciona enteira no teu móbil.

Non pide o permiso de internet. Iso non é unha promesa: é unha restrición do propio sistema operativo, e calquera pode comprobalo no manifesto da aplicación. Os teus documentos non poden saír do dispositivo porque a app non ten forma de envialos a ningunha parte.

QUE FAI

• Unir documentos nun só, arrastrando para ordenalos e coa vista previa da primeira páxina de cada un. Admite tamén Word, Excel, PowerPoint e imaxes: o que non é PDF convértese antes.
• Separar un documento nun ficheiro por páxina, por rangos ou extraendo as páxinas que elixas.
• Converter fotos en documentos, desde a galería ou facendo a foto no momento. Unha imaxe por páxina, ou 2, 4 e 6 xuntas.
• Editar páxinas: debuxar a man, resaltar, engadir formas, frechas, caixas de texto e imaxes. Substituír unha liña do texto que xa hai. Aplicar filtros para deixar limpo un documento fotografado.
• Asinar a man ou co teu propio certificado. A sinatura con certificado queda incrustada para que calquera lector de PDF poida comprobar que o documento non cambiou desde que o asinaches.
• Reordenar páxinas arrastrando, vendo o resultado antes de aplicalo.
• Converter entre PDF e Word, Excel e PowerPoint, nos dous sentidos.

PRIVACIDADE, SEN ADORNOS

• Sen permiso de internet. Sen ningún permiso, de feito.
• Sen contas, sen rexistro, sen analítica, sen informes de fallos e sen publicidade.
• O teu certificado lese, úsase e descártase. Non se garda nunca.
• O cartafol de traballo baléirase cada vez que arranca a aplicación.
• Os ficheiros que creas gárdanse en Descargas/NexaPDF, no teu propio teléfono.

SINCERIDADE SOBRE A CONVERSIÓN

Converter entre PDF e os formatos de ofimática conserva o contido, non o deseño exacto. Un PDF garda letras colocadas sobre unha páxina, non parágrafos nin celas, así que a estrutura reconstrúese o mellor posible. A aplicación dicho antes de converter en vez de prometer algo que non pode cumprir.

PENSADA PARA USALA A GUSTO

Seis temas, tres claros e tres escuros, máis a opción de seguir o sistema. Trece idiomas, co árabe de dereita a esquerda. Descricións para lectores de pantalla e áreas táctiles que respectan as guías de accesibilidade.

DE BALDE, E ASÍ SEGUIRÁ

Todas as funcións están dispoñibles para todo o mundo. Non hai contido bloqueado, nin funcións reservadas, nin unha segunda versión. Se a aplicación che resulta útil, nos axustes hai unha forma voluntaria de dar as grazas. Non cambia nada: todo segue igual de dispoñible para todo o mundo.

Código fonte: github.com/braisgaldo/NexaPDF""",

    "ca": """NexaPDF és una caixa d'eines per a PDF que funciona sencera al teu mòbil.

No demana el permís d'internet. Això no és una promesa: és una restricció del mateix sistema operatiu, i qualsevol pot comprovar-ho al manifest de l'aplicació. Els teus documents no poden sortir del dispositiu perquè l'aplicació no té cap manera d'enviar-los enlloc.

QUÈ FA

• Unir documents en un de sol, arrossegant per ordenar-los i amb la previsualització de la primera pàgina de cadascun. Admet també Word, Excel, PowerPoint i imatges: el que no és PDF es converteix abans.
• Dividir un document en un fitxer per pàgina, per intervals o extraient les pàgines que triïs.
• Convertir fotos en documents, des de la galeria o fent la foto al moment. Una imatge per pàgina, o 2, 4 i 6 juntes.
• Editar pàgines: dibuixar a mà, ressaltar, afegir formes, fletxes, quadres de text i imatges. Substituir una línia del text que ja hi ha. Aplicar filtres per deixar net un document fotografiat.
• Signar a mà o amb el teu propi certificat. La signatura amb certificat queda incrustada perquè qualsevol lector de PDF pugui comprovar que el document no ha canviat des que el vas signar.
• Reordenar pàgines arrossegant, veient el resultat abans d'aplicar-lo.
• Convertir entre PDF i Word, Excel i PowerPoint, en tots dos sentits.

PRIVADESA, SENSE ADORNS

• Sense permís d'internet. Sense cap permís, de fet.
• Sense comptes, sense registre, sense analítica, sense informes d'errors i sense publicitat.
• El teu certificat es llegeix, s'utilitza i es descarta. No es desa mai.
• La carpeta de treball es buida cada vegada que s'engega l'aplicació.
• Els fitxers que crees es desen a Baixades/NexaPDF, al teu propi telèfon.

SINCERITAT SOBRE LA CONVERSIÓ

Convertir entre PDF i els formats ofimàtics conserva el contingut, no el disseny exacte. Un PDF desa lletres col·locades sobre una pàgina, no pas paràgrafs ni cel·les, així que l'estructura es reconstrueix al màxim possible. L'aplicació t'ho diu abans de convertir en comptes de prometre allò que no pot complir.

PENSADA PERQUÈ S'USI A GUST

Sis temes, tres clars i tres foscos, més l'opció de seguir el sistema. Tretze idiomes, amb l'àrab de dreta a esquerra. Descripcions per a lectors de pantalla i àrees tàctils que respecten les guies d'accessibilitat.

GRATUÏTA, I AIXÍ CONTINUARÀ

Totes les funcions estan disponibles per a tothom. No hi ha contingut bloquejat, ni funcions reservades, ni una segona versió. Si l'aplicació et resulta útil, a la configuració hi ha una manera voluntària de donar les gràcies. No canvia res: tot continua igual de disponible per a tothom.

Codi font: github.com/braisgaldo/NexaPDF""",

    "eu": """NexaPDF zure mugikorrean osorik dabilen PDF tresna-kutxa bat da.

Ez du interneterako baimenik eskatzen. Hori ez da promesa bat: sistema eragileak berak jarritako muga da, eta edonork egiazta dezake aplikazioaren manifestuan. Zure dokumentuek ezin dute gailutik atera, aplikazioak ez baitu inora bidaltzeko modurik.

ZER EGITEN DUEN

• Dokumentuak bakarrean batu, arrastatuz ordenatuz eta bakoitzaren lehen orriaren aurrebistarekin. Word, Excel, PowerPoint eta irudiak ere onartzen ditu: PDF ez dena lehenago bihurtzen da.
• Dokumentu bat zatitu: fitxategi bat orriko, barrutika, edo aukeratutako orriak aterata.
• Argazkiak dokumentu bihurtu, galeriatik edo unean bertan aterata. Irudi bat orriko, edo 2, 4 eta 6 elkarrekin.
• Orriak editatu: eskuz marraztu, nabarmendu, formak, geziak, testu-koadroak eta irudiak gehitu. Dagoen testuaren lerro bat ordeztu. Iragazkiak aplikatu argazkiz ateratako dokumentu bat garbitzeko.
• Eskuz sinatu, edo zeure ziurtagiriarekin. Ziurtagiriarekiko sinadura kapsulatuta geratzen da, eta edozein PDF irakurgailuk egiazta dezake dokumentua ez dela aldatu sinatu zenuenetik.
• Orriak arrastatuz berrantolatu, emaitza aplikatu aurretik ikusiz.
• PDF eta Word, Excel eta PowerPoint artean bihurtu, bi noranzkoetan.

PRIBATUTASUNA, BIRIBILKETARIK GABE

• Interneterako baimenik ez. Egia esan, baimenik bat ere ez.
• Konturik ez, izen-ematerik ez, analitikarik ez, akats-txostenik ez, iragarkirik ez.
• Zure ziurtagiria irakurri, erabili eta baztertu egiten da. Ez da inoiz gordetzen.
• Lan-karpeta hustu egiten da aplikazioa abiarazten den bakoitzean.
• Sortzen dituzun fitxategiak Deskargak/NexaPDF karpetan gordetzen dira, zure telefonoan.

BIHURKETARI BURUZKO ZINTZOTASUNA

PDF eta bulegotika-formatuen arteko bihurketak edukia gordetzen du, ez diseinu zehatza. PDF batek orri batean jarritako letrak gordetzen ditu, ez paragrafoak eta gelaxkak, beraz egitura ahalik eta ondoen berreraikitzen da. Aplikazioak bihurtu aurretik esaten dizu, bete ezin duena agindu beharrean.

EROSO ERABILTZEKO EGINA

Sei gai, hiru argi eta hiru ilun, gehi sistemari jarraitzeko aukera. Hamahiru hizkuntza, arabiera eskuinetik ezkerrera. Pantaila-irakurgailuentzako deskribapenak eta irisgarritasun-gidalerroak errespetatzen dituzten ukipen-eremuak.

DOAN, ETA HALA JARRAITUKO DU

Funtzio guztiak eskuragarri daude denentzat. Ez dago eduki blokeaturik, ez funtzio erreserbaturik, ez bigarren bertsiorik. Aplikazioa erabilgarria bazaizu, ezarpenetan eskerrak emateko modu boluntario bat dago. Ez da ezer aldatzen: dena lehen bezain eskuragarri jarraitzen du.

Iturburu-kodea: github.com/braisgaldo/NexaPDF""",
}
