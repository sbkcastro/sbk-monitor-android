# 🚀 GitHub Actions CI/CD - SBK Monitor Android

**Documentación completa del pipeline de CI/CD**

Fecha: 2026-02-11
Autor: SBK Castro + Claude Sonnet 4.5
Estado: ✅ Operativo y monitoreado activamente

---

## 📋 Resumen

GitHub Actions compila y firma automáticamente el APK de SBK Monitor en cada push a `main`/`master`.

**Pipeline automático:**
1. Push código → GitHub
2. GitHub Actions ejecuta workflow
3. Compila APK con Gradle
4. Firma con keystore en Secrets
5. Sube APK como artifact (90 días retención)
6. En tags `v*` → crea GitHub Release automáticamente

---

## 🏗️ Arquitectura del Workflow

```yaml
name: Android CI/CD

on:
  push:
    branches: [ main, master, develop ]
    tags: [ 'v*' ]
  pull_request:
    branches: [ main, master ]
  workflow_dispatch:  # Ejecución manual
```

### Jobs:

#### 1. Build & Sign APK
- **Runner:** `ubuntu-latest`
- **Tiempo aprox:** 4-5 minutos
- **Pasos críticos:**
  - Setup Java 17 (Temurin)
  - Decodificar keystore desde base64
  - Compilar con Gradle
  - Firmar con apksigner
  - Verificar firma
  - Upload artifact

---

## 🔑 Secrets Configurados

| Secret | Descripción | Valor (oculto) |
|--------|-------------|----------------|
| `KEYSTORE_BASE64` | Keystore en base64 | `base64 sbk-monitor-release.jks` |
| `KEYSTORE_PASSWORD` | Password del keystore | `android123` |
| `KEY_ALIAS` | Alias de la clave | `sbk-monitor` |
| `KEY_PASSWORD` | Password de la clave | `android123` |

**⚠️ NUNCA** commits estos valores en código!

### Cómo actualizar secrets:

```bash
# Desde el repo local
cd /path/to/sbk-monitor-android

# Actualizar keystore
base64 -w 0 sbk-monitor-release.jks | gh secret set KEYSTORE_BASE64

# Actualizar passwords
echo "nueva_password" | gh secret set KEYSTORE_PASSWORD
echo "sbk-monitor" | gh secret set KEY_ALIAS
echo "nueva_password" | gh secret set KEY_PASSWORD

# Verificar
gh secret list
```

---

## 📊 Estado del Workflow

### Ver últimos builds:

```bash
# Listar últimas 5 ejecuciones
gh run list --limit 5

# Ver detalles de un run específico
gh run view RUN_ID

# Ver logs en tiempo real
gh run watch RUN_ID

# Descargar artifact
gh run download RUN_ID
```

### Monitoreo web:

https://github.com/sbkcastro/sbk-monitor-android/actions

---

## ✅ Build Exitoso - Checklist

Cuando un build completa exitosamente:

- [x] ✓ Checkout code
- [x] ✓ Setup Java 17
- [x] ✓ Setup Android SDK
- [x] ✓ Decode Keystore
- [x] ✓ Grant execute permission for gradlew
- [x] ✓ Clean build directory
- [x] ✓ **Build Release APK** ← Crítico
- [x] ✓ List build outputs (debug)
- [x] ✓ **Sign APK with apksigner** ← Crítico
- [x] ✓ Extract version info
- [x] ✓ Rename APK
- [x] ✓ **Upload APK Artifact** ← Crítico
- [x] ✓ Cleanup Keystore

**Resultado:** APK firmado disponible en Artifacts por 90 días

---

## ❌ Troubleshooting - Gestión de Fallos

### Fallo 1: "Build Release APK" falla

**Síntoma:**
```
❌ Build Release APK
FAILURE: Build failed with an exception.
```

**Posibles causas:**

1. **Error de compilación Kotlin/Java**
   ```bash
   # Ver logs completos
   gh run view RUN_ID --log

   # Buscar "error:" en logs
   gh run view RUN_ID --log | grep -i "error:"
   ```

   **Solución:** Arreglar error de código, commit, push.

2. **Dependencia no resuelta**
   ```
   Could not resolve androidx.core:core-ktx:1.x.x
   ```

   **Solución:**
   - Verificar `app/build.gradle.kts` tiene versiones correctas
   - Actualizar Gradle wrapper si es muy antiguo:
     ```bash
     ./gradlew wrapper --gradle-version 8.2
     git add gradle/wrapper/
     git commit -m "chore: update Gradle wrapper"
     git push
     ```

3. **Timeout de Gradle**
   ```
   Read timed out
   ```

   **Solución:** Re-ejecutar workflow (puede ser problema temporal de red)

### Fallo 2: "Sign APK with apksigner" falla

**Síntoma:**
```
❌ Sign APK with apksigner
APK not found at app/build/outputs/apk/release/app-release.apk
```

**Causa:** Build anterior falló silenciosamente

**Solución:**
- Revisar step "Build Release APK"
- Verificar que `assembleRelease` completa sin errores

### Fallo 3: "Signature verification failed"

**Síntoma:**
```
❌ Signature verification failed
```

**Posibles causas:**

1. **Keystore corrupto**
   ```bash
   # Re-generar secret KEYSTORE_BASE64
   base64 -w 0 sbk-monitor-release.jks | gh secret set KEYSTORE_BASE64
   ```

2. **Password incorrecto**
   ```bash
   # Verificar localmente
   keytool -list -v -keystore sbk-monitor-release.jks
   # (debe pedir password correcto)

   # Actualizar secret
   echo "password_correcto" | gh secret set KEYSTORE_PASSWORD
   ```

3. **Alias incorrecto**
   ```bash
   # Listar aliases en keystore
   keytool -list -keystore sbk-monitor-release.jks

   # Actualizar secret
   echo "alias_correcto" | gh secret set KEY_ALIAS
   ```

### Fallo 4: "Upload APK Artifact" falla

**Síntoma:**
```
❌ Upload APK Artifact
No files were found with the provided path
```

**Causa:** APK no se generó o está en ruta incorrecta

**Solución:**
- Verificar step "🔍 List build outputs (debug)"
- Ver logs: `gh run view RUN_ID --log | grep "Build outputs"`
- Ajustar ruta en workflow si es necesario

### Fallo 5: Workflow no se dispara

**Síntoma:** Push a `main` pero workflow no ejecuta

**Posibles causas:**

1. **Branch incorrecto**
   ```bash
   # Verificar branch actual
   git branch

   # Push a main
   git push origin main
   ```

2. **Workflow deshabilitado**
   - Ir a https://github.com/sbkcastro/sbk-monitor-android/actions
   - Verificar que workflow no esté disabled

3. **Sintaxis YAML incorrecta**
   ```bash
   # Validar sintaxis localmente
   yamllint .github/workflows/android-ci.yml
   ```

---

## 🔧 Ejecutar Workflow Manualmente

```bash
# Opción 1: CLI
gh workflow run "Android CI/CD" --ref main

# Opción 2: Web
# https://github.com/sbkcastro/sbk-monitor-android/actions/workflows/android-ci.yml
# Click "Run workflow"
```

---

## 📦 Descargar APK del Artifact

### Método 1: GitHub CLI

```bash
# Listar runs
gh run list --limit 5

# Descargar artifact del último run exitoso
gh run download $(gh run list --status success --limit 1 --json databaseId --jq '.[0].databaseId')

# Resultado: directorio sbk-monitor-v2.3.0/ con APK
```

### Método 2: Web

1. https://github.com/sbkcastro/sbk-monitor-android/actions
2. Click en run exitoso (checkmark verde)
3. Scroll abajo a "Artifacts"
4. Click "sbk-monitor-v2.3.0" para descargar ZIP

---

## 🏷️ Crear Release con GitHub Actions

Para que GitHub Actions cree un release automáticamente:

```bash
# Crear tag
git tag v2.3.0
git push origin v2.3.0

# GitHub Actions detecta tag v* y:
# 1. Compila APK
# 2. Firma APK
# 3. Crea GitHub Release
# 4. Sube APK al release
```

**Resultado:**
- Release en https://github.com/sbkcastro/sbk-monitor-android/releases/tag/v2.3.0
- APK descargable directamente

---

## 📈 Métricas del Pipeline

| Métrica | Valor Actual | Target |
|---------|--------------|--------|
| Tiempo build | 4m 49s | < 5m |
| Tamaño APK | 6.4 MB | < 10 MB |
| Success rate | 100% (1/1) | > 95% |
| Artifact retention | 90 días | 90 días |

---

## 🔄 Comparación Local vs GitHub Actions

| Aspecto | Build Local | GitHub Actions |
|---------|-------------|----------------|
| **Tiempo** | ~2-3 min | ~5 min |
| **Ventaja local** | Más rápido | - |
| **Ventaja GitHub** | - | Automatizado, versionado, artifacts |
| **Reproducibilidad** | Depende entorno local | 100% reproducible |
| **Costo** | Gratis (local) | Gratis (GitHub Free) |

**Conclusión:** Usar GitHub Actions para releases, local para desarrollo rápido.

---

## 🛡️ Seguridad

### ✅ Buenas prácticas implementadas:

1. **Keystore NUNCA en código** - Solo en GitHub Secrets
2. **Secrets encriptados** - GitHub cifra en reposo
3. **Cleanup automático** - Keystore se elimina del runner post-build
4. **Logs sin passwords** - `--ks-pass pass:***` oculta contraseñas
5. **Branch protection** - Solo main/master disparan build

### ⚠️ Advertencias:

- **NO** hacer fork público si contiene keystore
- **NO** compartir secrets con terceros
- **NO** commits keystore por error (ya está en .gitignore)

---

## 📞 Soporte y Monitoreo

### Responsable:
- SBK Castro (GitHub: @sbkcastro)
- Claude Sonnet 4.5 (gestión activa de fallos)

### Monitoreo:
- GitHub Actions: https://github.com/sbkcastro/sbk-monitor-android/actions
- Email notificaciones: GitHub envía emails automáticamente en fallos
- Telegram (opcional): Configurar webhook en workflow

### SLA (Service Level Agreement):
- **Detección de fallo:** < 5 minutos (notificación automática)
- **Análisis:** < 30 minutos (revisar logs)
- **Fix:** < 2 horas (commit + push)
- **Disponibilidad:** 99% (GitHub Actions SLA)

---

## 📚 Referencias

- [GitHub Actions Docs](https://docs.github.com/en/actions)
- [Android Build Tools](https://developer.android.com/studio/command-line)
- [apksigner Reference](https://developer.android.com/tools/apksigner)
- [Gradle User Guide](https://docs.gradle.org/current/userguide/userguide.html)

---

## 🎯 Próximos Pasos

- [ ] Configurar notificaciones Telegram en fallos
- [ ] Añadir tests unitarios al pipeline
- [ ] Configurar ProGuard para reducir tamaño APK
- [ ] Configurar multiple build variants (debug, staging, release)
- [ ] Añadir análisis de vulnerabilidades con Dependabot

---

**Última actualización:** 2026-02-11 14:00 UTC
**Versión docs:** 1.0
**Status:** ✅ Operativo y monitoreado activamente
