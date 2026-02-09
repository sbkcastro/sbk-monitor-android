# 🖥️ SBK Monitor - Android App

**Monitoreo del servidor en tiempo real con inteligencia artificial**

[![Version](https://img.shields.io/badge/version-1.1.0-blue.svg)](https://github.com/sbkcastro/sbk-monitor-android/releases)
[![Platform](https://img.shields.io/badge/platform-Android%206%2B-green.svg)](https://www.android.com/)
[![License](https://img.shields.io/badge/license-MIT-orange.svg)](LICENSE)

## 📱 Descripción

SBK Monitor es una aplicación Android para monitorear y gestionar el servidor **31.97.55.27** que aloja:
- **sbkcastro.com** - Portfolio personal
- **eventos.sbkcastro.com** - Sistema de gestión de eventos
- **dj.sbkcastro.com** - Aplicación DJ con Spotify

La app se conecta a una API REST y permite interactuar con **OpenClaw**, un asistente AI con acceso real al servidor.

## ✨ Características

### 🤖 Chat con OpenClaw AI
- **GPT-4o-mini** vía OpenRouter con function calling
- **Herramientas reales**: ejecuta comandos bash, verifica sitios web, obtiene métricas
- **Detección de mineros** automática
- Respuestas en tiempo real con datos del servidor

### 📊 Métricas en Tiempo Real
- **CPU, RAM, Disco** actualizados cada 30 segundos
- **Gráficos históricos** de últimas 4 horas
- **Widget de Android** con colores dinámicos
- **Containers LXC/Docker** con estado e IPs

### 🔒 Seguridad
- **Detección automática de mineros** cada 6 horas
- Notificaciones push ante actividad sospechosa
- Autenticación JWT con tokens encriptados
- Whitelist/blacklist de comandos

### 🌐 Monitoreo de Sitios
- Estado HTTP de todos los sitios
- Alertas si algún sitio cae
- Restart de containers desde la app

## 🚀 Instalación

### Opción 1: Descargar APK
1. Ir a [Releases](https://github.com/sbkcastro/sbk-monitor-android/releases)
2. Descargar `sbk-monitor-v1.1.0.apk`
3. Instalar (permitir instalación de fuentes desconocidas)

### Opción 2: Compilar desde código

```bash
# Clonar repositorio
git clone https://github.com/sbkcastro/sbk-monitor-android.git
cd sbk-monitor-android

# Compilar con Gradle
./gradlew assembleDebug

# APK generada en:
# app/build/outputs/apk/debug/app-debug.apk
```

**Requisitos:**
- Android Studio Arctic Fox o superior
- JDK 17
- Android SDK 26+

## 📖 Uso

### Primera vez
1. Abrir la app
2. Introducir credenciales:
   - **Usuario:** `sbk`
   - **Password:** (solicitar al admin)
3. Seleccionar backend: **OpenClaw** o **Claude Code**

### Chat
```
Tú: Dame el estado del servidor
OpenClaw: El estado actual es:
• CPU: 12.3%
• RAM: 45.2%
• Disco: 67%
• Uptime: 2 semanas, 6 días
```

### Detección de mineros
```
Tú: Detecta mineros
OpenClaw: ✅ No se detectaron mineros ni procesos sospechosos.

Top 10 procesos por CPU:
node server.js - 2.5%
claude - 1.8%
...
```

## 🛠️ Arquitectura

```
┌─────────────────────────────────────────┐
│         Android App (Kotlin)            │
│  ┌────────────┐  ┌──────────────────┐  │
│  │  UI Layer  │  │  Data Layer      │  │
│  │  Jetpack   │  │  Retrofit + GSON │  │
│  │  Compose   │  │  WorkManager     │  │
│  └────────────┘  └──────────────────┘  │
└───────────────┬─────────────────────────┘
                │ HTTPS + JWT Auth
                ▼
┌─────────────────────────────────────────┐
│    API REST (Node.js + Express)         │
│    https://monitor.sbkcastro.com/api    │
│  ┌────────────┐  ┌──────────────────┐  │
│  │ Auth       │  │ OpenClaw Chat    │  │
│  │ Metrics    │  │ + Function Call  │  │
│  │ Containers │  │ + GPT-4o-mini    │  │
│  └────────────┘  └──────────────────┘  │
└───────────────┬─────────────────────────┘
                │ Bash Commands
                ▼
┌─────────────────────────────────────────┐
│      Servidor (31.97.55.27)             │
│  ┌────────┐  ┌────────┐  ┌────────┐    │
│  │LXC-SBK │  │LXC-DJ  │  │LXC-DEV │    │
│  │Docker  │  │Docker  │  │Docker  │    │
│  └────────┘  └────────┘  └────────┘    │
└─────────────────────────────────────────┘
```

## 📂 Estructura del Proyecto

```
sbk-monitor-android/
├── app/
│   ├── src/main/java/com/sbkcastro/monitor/
│   │   ├── MainActivity.kt              # Activity principal
│   │   ├── api/
│   │   │   ├── ApiService.kt           # Retrofit API
│   │   │   ├── ApiClient.kt            # Cliente HTTP
│   │   │   └── models/                 # Data classes
│   │   ├── ui/
│   │   │   ├── home/HomeFragment.kt    # Métricas
│   │   │   ├── chat/ChatFragment.kt    # Chat AI
│   │   │   ├── charts/ChartsFragment.kt # Gráficos
│   │   │   └── containers/             # LXC/Docker
│   │   ├── worker/
│   │   │   └── MinerScanWorker.kt      # Detección mineros
│   │   └── widget/
│   │       └── ServerWidgetProvider.kt # Widget
│   ├── src/main/res/
│   │   ├── layout/                     # XMLs
│   │   ├── navigation/                 # Nav graph
│   │   └── values/                     # Strings, colors
│   └── build.gradle.kts
├── build.gradle.kts
├── settings.gradle.kts
└── README.md
```

## 🔧 Tecnologías

### Android
- **Kotlin** 1.9+
- **Jetpack Navigation** - Navegación entre fragments
- **ViewModel + LiveData** - Arquitectura MVVM
- **WorkManager** - Tareas en background
- **Retrofit2 + OkHttp3** - Cliente HTTP
- **GSON** - Serialización JSON
- **MPAndroidChart** - Gráficos
- **EncryptedSharedPreferences** - Almacenamiento seguro

### Backend
- **Node.js + Express** - API REST
- **OpenRouter** - Gateway a GPT-4o-mini
- **JWT** - Autenticación
- **LXC + Docker** - Containers

## 📊 Métricas de Código

| Métrica | Valor |
|---------|-------|
| Lenguaje | Kotlin 100% |
| Tamaño APK | 7.9 MB |
| Min SDK | Android 8.0 (API 26) |
| Target SDK | Android 14 (API 34) |
| Archivos .kt | 45+ |
| Dependencias | 15 |

## 🤝 Contribuir

Aunque es un proyecto personal, las sugerencias son bienvenidas:

1. Fork el repositorio
2. Crea una rama: `git checkout -b feature/nueva-feature`
3. Commit: `git commit -m 'feat: añadir nueva feature'`
4. Push: `git push origin feature/nueva-feature`
5. Abre un Pull Request

## 📝 Changelog

### [v1.1.0] - 2026-02-09

#### Añadido
- 📊 Gráficos históricos con MPAndroidChart
- 🔒 Detección automática de mineros cada 6h
- 🤖 OpenClaw con function calling (4 herramientas)
- 🎨 Widget con colores dinámicos
- 📱 Notificaciones push para alertas

#### Mejorado
- ⚡ System prompt optimizado (80% menos tokens)
- 🔧 Backend con whitelist/blacklist de comandos
- 🛡️ Mejor manejo de errores

### [v1.0.0] - 2026-02-08

#### Inicial
- ✅ Chat con OpenClaw AI
- ✅ Métricas en tiempo real
- ✅ Lista de containers LXC/Docker
- ✅ Autenticación JWT
- ✅ Widget de Android

## 📄 Licencia

MIT License - Ver [LICENSE](LICENSE) para más detalles.

## 👤 Autor

**SBK Castro**
- GitHub: [@sbkcastro](https://github.com/sbkcastro)
- Website: [sbkcastro.com](https://sbkcastro.com)
- Servidor: 31.97.55.27

## 🙏 Agradecimientos

- **Claude Sonnet 4.5** - Co-desarrollo de la app
- **OpenRouter** - Gateway a modelos LLM
- **PhilJay** - [MPAndroidChart](https://github.com/PhilJay/MPAndroidChart)

---

**Compilado con ❤️ usando Android Studio y Claude Code**

🤖 *Esta app fue desarrollada con asistencia de Claude Sonnet 4.5*
