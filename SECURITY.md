# Política de seguridad

## Versiones con soporte

Se da soporte a la última versión publicada en Google Play y en las Releases de
este repositorio. Las versiones anteriores no reciben parches.

| Versión | Soporte |
|---|---|
| 1.0.x | Sí |

## Cómo informar de un fallo de seguridad

**No abras una incidencia pública.** Escribe a **ghatostudio@proton.me** con:

- Qué versión de NexaPDF y qué versión de Android.
- Qué ocurre y cómo reproducirlo.
- Qué impacto crees que tiene.

Respondo en un plazo de siete días. Si el fallo es real, publico la corrección y
te menciono en el `CHANGELOG.md`, salvo que prefieras lo contrario.

## Superficie de ataque de NexaPDF

Conviene saber qué hay y qué no hay, porque acota mucho lo que puede fallar:

- **No hay red.** La aplicación no declara `INTERNET`. No hay servidor, ni API,
  ni actualizaciones descargadas, ni telemetría.
- **No hay cuentas.** No hay autenticación, ni sesiones, ni tokens.
- **No hay permisos.** El manifiesto no declara ninguno.
- **Lo que sí procesa** son ficheros que el usuario elige: PDF, documentos
  OOXML e imágenes. Ahí está el riesgo real: un fichero malformado que provoque
  un fallo en PDFBox o en el analizador de OOXML. Toda la lectura de ficheros
  está envuelta en manejo de errores que devuelve un mensaje al usuario en lugar
  de propagar la excepción.
- **Criptografía**: firma PKCS#7 con BouncyCastle. El certificado del usuario se
  lee, se usa y se descarta; no se guarda ni se copia.

## Secretos del proyecto

El keystore de firma y sus contraseñas **no están en el repositorio** y nunca
deben estarlo. Viven en `keystore.properties` y en `docs/google_play/claves/`,
ambos ignorados por git, y en los *secrets* de GitHub Actions para la
compilación de release.

El procedimiento para regenerarlos, y qué hacer si se pierden, está en
`docs/INSTALL.md`.
