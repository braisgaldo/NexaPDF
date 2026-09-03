# NexaPDF

**Herramientas PDF que funcionan sin conexión.** Une, separa, convierte, edita y
firma documentos sin que salgan de tu teléfono.

NexaPDF no declara el permiso de internet. No es una promesa de buena voluntad:
es una restricción del propio sistema operativo, comprobable en el manifiesto.

---

## Qué hace

| | |
|---|---|
| **Unir** | Varios documentos en uno, arrastrando para ordenarlos. Admite PDF, Word, Excel, PowerPoint e imágenes: lo que no sea PDF se convierte antes. |
| **Separar** | Por páginas sueltas, por rangos o extrayendo una selección. |
| **Imágenes a PDF** | Desde la galería o haciendo una foto. Una por página, o 2, 4 y 6 juntas. |
| **Editar** | Dibujar a mano, resaltar, formas, texto (sustituyendo el existente o añadiendo nuevo), incrustar imágenes y filtros de mejora de página. |
| **Firmar** | A mano, o con un certificado `.p12`/`.pfx` produciendo una firma verificable por cualquier lector de PDF. |
| **Convertir** | PDF ↔ Word, Excel y PowerPoint, en los dos sentidos. |
| **Reordenar** | Páginas y documentos, arrastrando, con vista previa real de cada uno. |

Seis temas (tres claros y tres oscuros) más «seguir al sistema», y trece idiomas
con el árabe en RTL.

## Capturas

Las capturas del dispositivo real están en [`docs/capturas/`](docs/capturas/).

## Instalar

- **Google Play**: <https://play.google.com/store/apps/details?id=es.ghatostudio.nexapdf>
- **APK directo**: en las [Releases](https://github.com/braisgaldo/NexaPDF/releases)
  de este repositorio. Sin tienda de por medio y sin ninguna política de
  facturación que aplique.

## Compilar

Ver [`docs/INSTALL.md`](docs/INSTALL.md) para los requisitos exactos y el
procedimiento completo, incluida la firma de release.

```bash
git clone https://github.com/braisgaldo/NexaPDF.git
cd NexaPDF
./gradlew :composeApp:assembleDebug
```

## Arquitectura

Kotlin Multiplatform con Compose Multiplatform: una sola base de código para
Android, con la interfaz y el dominio ya preparados para iOS y escritorio.
`commonMain` no contiene ni una sola referencia a Android.

Los detalles y el porqué de cada decisión están en
[`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) y en los
[ADR](docs/adr/).

## Privacidad

NexaPDF no recoge ningún dato. La política completa está en
[`docs/PRIVACIDAD.md`](docs/PRIVACIDAD.md) y publicada en
<https://braisgaldo.github.io/NexaPDF/privacidad.html>.

## Apoyar el desarrollo

NexaPDF es gratuita, sin anuncios y sin seguimiento, y lo seguirá siendo. Si te
resulta útil, puedes **[invitarme a un café](https://revolut.me/brais2oz6)**.

La donación es voluntaria y **no desbloquea nada**: no hay funciones de pago, ni
versión «pro», ni contenido reservado. Todo lo que hace la aplicación lo hace
para todo el mundo.

## Licencia

[MIT](LICENSE). Las licencias de las bibliotecas de terceros están listadas en
la pantalla «Acerca de» de la aplicación y en `docs/ARCHITECTURE.md`.
