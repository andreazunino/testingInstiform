# Selenium + Java + Maven: Herramientas CLI con IA y Self‑Healing para Creación y Mantenimiento de Tests (con Integración a Jira)
Se reume como utilizar estas cinco tecnologías desde la línea de comandos (CLI) o como librerías para asistir la creación y el mantenimiento de tests automatizados en un stack Selenium + Java + Maven, con integración a Jira. Se explican su funcionamiento, arquitectura, setup, flujos de uso local y en CI/CD, y patrones de integración con Jira. Las herramientas cubiertas son: Gemini CLI, OpenAI CLI, GitHub Copilot CLI, Anthropic Claude CLI y Healenium.
Contexto de proyecto (Selenium + Java + Maven)
El objetivo es reducir el tiempo que se invierte en reparar tests por cambios de UI (selectores, tiempos de carga, flujo), y mejorar la estabilidad/velocidad del pipeline. Para eso combinamos:
• Selenium + Java (tests y Page Objects)
• Maven (build, dependencias, ejecución en CI)
• Una IA CLI (Gemini / OpenAI / Copilot / Claude) para analizar fallos, proponer fixes y generar artefactos (JSON con nuevos selectores, parches)
• Self‑healing con Healenium para curar selectores en tiempo de ejecución
• Jira para trazabilidad (issues, adjuntos, links a PRs y ejecuciones)

# ui-tests-healenium

Automatización UI con **Selenium + TestNG + Healenium**, generación de artefactos (DOM/screenshot/logs), y (opcional) creación de issues en **Jira**. Incluye backend de Healenium en **Docker** y ejecución local o 100% en contenedores.

---

## 🧭 TL;DR

1. **Healenium (Docker)**

   ```powershell
   cd infra/healenium
   docker compose up -d
   ```

   UI: [http://localhost:7878/healenium/report](http://localhost:7878/healenium/report)

2. **Tests (local, Edge por defecto)**

   ```powershell
   cd ../../ui-tests-healenium
   $env:CI="true"
   .\mvnw.cmd -B -ntp clean test "-DBASE_URL=https://mi-app" "-DBROWSER=edge"
   ```

3. **Artifacts** (en fallos): `artifacts/<Clase>__<Método>__<timestamp>/`

4. **Jira (opcional)**

   ```powershell
   $env:JIRA_BASE_URL="https://tuorg.atlassian.net"
   $env:JIRA_USER_EMAIL="tu@email"
   $env:JIRA_API_TOKEN="***"
   $env:JIRA_PROJECT_KEY="QA"
   $env:JIRA_AUTO_CREATE="true"
   ```

---

## 📦 Requisitos (una sola vez)

* **Docker Desktop** (o Docker Engine)
* **JDK 21+** (recomendado 21 LTS)

  * Windows: `winget install -e --id EclipseAdoptium.Temurin.21.JDK`
  * Verificar: `java -version`
* **VS Code** + *Extension Pack for Java* (recomendado para autocompletado/import)
* **No hace falta Maven**: usamos **Maven Wrapper** (`mvnw`)

> Si tu empresa usa proxy, configurar Docker/Java/Maven en consecuencia (ver sección *Proxy*).

---

## 🧰 Estructura

```
repo-root/
├─ infra/
│  ├─ healenium/                # docker-compose backend Healenium + DB
│  └─ tests/                    # (opcional) selenium grid + runner Maven
└─ ui-tests-healenium/
   ├─ pom.xml                   # Maven (target 21, TestNG, Surefire)
   ├─ mvnw / mvnw.cmd           # Maven Wrapper
   ├─ src/test/java/
   │  ├─ qa/andrea/core/BaseTest.java
   │  ├─ qa/andrea/pages/...    # Page Objects
   │  ├─ qa/andrea/tests/...    # @Tests (TestNG)
   │  └─ qa/andrea/support/TestListener.java
   ├─ src/test/resources/
   │  ├─ testng.xml             # suite (registra TestListener)
   │  └─ healenium.properties   # URLs backend Healenium
   └─ artifacts/                # se crea en fallos
```

---

## 🚀 Primeros pasos

### 1) Clonar

```powershell
git clone <URL_DEL_REPO>
cd <carpeta-del-repo>
```

### 2) Backend Healenium (Docker)

```powershell
cd infra/healenium
docker compose up -d
docker compose ps
```

Abrí: `http://localhost:7878/healenium/report`

### 3) Ejecutar tests **locales**

```powershell
cd ../../ui-tests-healenium
$env:CI="true"  # headless + logs consistentes

# Edge (Windows) – recomendado para empezar
.\mvnw.cmd -B -ntp clean test "-DBASE_URL=https://mi-app" "-DBROWSER=edge"

# Chrome local
.\mvnw.cmd -B -ntp clean test "-DBASE_URL=https://mi-app" "-DBROWSER=chrome"
```

> El `pom.xml` ya fuerza **TestNG** vía Surefire y usa `src/test/resources/testng.xml`.

### 4) Ver resultados

* Consola: `Tests run: ...`.
* En fallos: `artifacts/<Clase>__<Método>__YYYYMMDD_HHMMSS/`

  * `screenshot.png`
  * `DOM.html`
  * `browser.log`
* Healenium dashboard: `http://localhost:7878/healenium/report`

---

## 🐳 Ejecutar **todo en Docker** (opcional)

Requiere que `BaseTest` soporte **RemoteWebDriver** cuando exista `REMOTE_GRID_URL`.

1. Levantar Selenium + Runner:

```powershell
cd infra/tests
docker compose up --build
```

* Servicio `selenium`: `:4444`
* Servicio `runner`: monta tu repo y ejecuta `./mvnw` apuntando al grid
* Healenium se consume por nombre de servicio desde la misma red Docker

**Artifacts** aparecen igualmente en `ui-tests-healenium/artifacts` (en tu host).

Para apagar:

```powershell
docker compose down
```

---

## ⚙️ Variables y parámetros

Se pueden definir como **vars de entorno** o `-Dproperty` al ejecutar Maven.

**Aplicación bajo prueba**

```
BASE_URL=https://mi-app.tuempresa.com
```

**Navegador / Grid**

```
BROWSER=edge|chrome
REMOTE_GRID_URL=http://localhost:4444/wd/hub   # activa modo Grid (Docker)
CI=true                                        # headless, etc.
```

**Healenium** (`src/test/resources/healenium.properties`)

```
hlm.server.url=http://localhost:7878
hlm.imitator.url=http://localhost:8000
score-cap=0.6
heal-enabled=true
recovery-tries=1
```

> En Docker, el runner sobreescribe esas URLs a `http://healenium:7878` / `http://selector-imitator:8000`.

**Jira (opcional)**

```
JIRA_BASE_URL=https://tuorg.atlassian.net
JIRA_USER_EMAIL=tu@email
JIRA_API_TOKEN=***
JIRA_PROJECT_KEY=QA
JIRA_AUTO_CREATE=true
```

Ejemplos:

```powershell
.\mvnw.cmd -B -ntp clean test `
  "-DBASE_URL=https://mi-app" "-DBROWSER=edge" "-Dsurefire.printSummary=true"
```

---

## 🧪 Cómo escribir y correr tests

* Ubicación: `src/test/java/qa/andrea/tests/…`
* Page Objects: `src/test/java/qa/andrea/pages/…` (locators `By.*` o `@FindBy`)
* Suite: `src/test/resources/testng.xml`
* Healenium “cura” selectores automáticamente al fallar (luego de haber aprendido en corridas verdes).

Comandos útiles:

```powershell
# Toda la suite (usa testng.xml)
.\mvnw.cmd -B -ntp clean test "-Dsurefire.suiteXmlFiles=src/test/resources/testng.xml"

# Por grupos TestNG (smoke, regression…)
.\mvnw.cmd -B -ntp clean test "-Dgroups=smoke" "-Dsurefire.suiteXmlFiles=src/test/resources/testng.xml"

# Por patrón de clase
.\mvnw.cmd -B -ntp -Dtest="*LoginTest" test
```

---

## 🐞 Jira (opcional)

Si `JIRA_AUTO_CREATE=true`, ante un fallo el listener:

* crea un **Bug** en el proyecto `JIRA_PROJECT_KEY`
* adjunta `screenshot.png`, `DOM.html` y `browser.log`

Requisitos: token de API, permisos de *Create Issue* y *Attach files*.

---

## 🧪 Healenium: flujo recomendado

1. Correr **smoke** verde para sembrar baseline de locators
2. Ante cambios de DOM, Healenium intentará *healing* según `score-cap` y `recovery-tries`
3. Revisar dashboard para ver curaciones y ajustar locators si fuera necesario

> Consejo: acordar con frontend usar **data-testid** para locators robustos.

---

## 🛠️ Troubleshooting rápido

* **“No tests to run”** → no hay clases en `src/test/java` o `testng.xml` apunta mal
* **`\ufeff illegal character`** → guardar `.java` en **UTF‑8 sin BOM**
* **Chrome no instalado** → usar `-DBROWSER=edge` o correr vía Grid
* **Healenium no levanta** → `docker compose logs` en `infra/healenium`; verificar puertos 7878/8000
* **Proxy corporativo** → configurar Docker y `~/.m2/settings.xml` (proxy + mirror Nexus/Artifactory)

---

## 🧱 Notas de CI (resumen)

* PR: suite **smoke** + cache Maven + artifacts de fallos
* Nightly: **regression** completa + publicar reporte + (opcional) crear ticket consolidado
* Matriz opcional: `BROWSER=edge|chrome`

---




