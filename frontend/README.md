## 1. Introducción

### Resumen del proyecto

Esta es la interfaz frontend de un gestor de validaciones de ficheros basado en React. Permite a los usuarios registrarse, iniciar sesión, subir archivos CSV, enviarlos a HDFS para su validación, visualizar los resultados de validación (logs), editar metadatos de cada configuración y descargar los ficheros validados.

### Explicación Aplicación

* Al abrir la aplicación sin token, se redirige automáticamente a la pantalla de Login.
* En la página de Registro (`/register`), el usuario crea su cuenta mediante email y contraseña.
* Tras registrarse, el usuario inicia sesión (`/login`), recibe un token JWT y es redirigido al Dashboard (`/dashboard`).
* En el Dashboard:

  1. Puede seleccionar un CSV y subirlo al backend.
  2. Visualiza una tabla con todas las configuraciones existentes (ID, nombre de fichero, ruta, delimitador, etc.).
  3. Cada fila incluye botones para **Validar** (envía a HDFS), **Detalles** (abre un modal con metadatos editables) y **Eliminar** (borra la configuración, tras confirmación).
* En el modal de **Detalles**, además de editar metadatos (delimiter, quote\_char, escape\_char, has\_header, date\_format, timestamp\_format, partition\_columns), puede:

  * **Guardar** cambios (PATCH).
  * **Validar** nuevamente (POST).
  * **Descargar** el CSV (mediante fetch + Blob).
  * **Eliminar** la configuración (DELETE).
* En la sección **Logs** (`/logs`), se muestra un listado de mensajes de validación con campos:

  * ID
  * file\_config\_id
  * file\_name
  * field\_name
  * environment
  * validation\_flag
  * error\_message
  * logged\_at (fecha formateada como DD/MM/YYYY, hh:mm:ss en zona Madrid)
* El botón **Logout** aparece en la esquina superior derecha (color naranja); al pulsarlo, borra el token y redirige a Login.

### Resumen de tecnologías utilizadas

* **React 18+**: biblioteca principal para componentes y hooks (`useState`, `useEffect`).
* **React Router v6**:

  * `<BrowserRouter>`
  * `<Routes>` / `<Route>`
  * `<Navigate>`
  * `useLocation()`
  * `<Outlet>`
* **Axios**: cliente HTTP con dos interceptores:

  * **Interceptor de petición** añade `Authorization: Bearer <token>` si existe en `localStorage`.
  * **Interceptor de respuesta** detecta **401 Unauthorized**, borra el token y redirige a `/login`.
* **Context API** (`AuthContext`): métodos `login()`, `register()`, `logout()` y estado `token`.
* **CSS puro**: todos los estilos globales en `src/styles/index.css`.
* **Vite**: bundler y servidor de desarrollo.

---

## 2. Especificación de Requisitos

### Requisitos funcionales

1. **Autenticación**

   * RF1.1: Registro de usuario con email y contraseña (`POST /auth/register`).
   * RF1.2: Login de usuario (`POST /auth/login`), recibe JWT y redirige a `/dashboard`.
   * RF1.3: Logout que borra token de `localStorage` y redirige a `/login`.
   * RF1.4: Cualquier petición protegida que retorne **401 Unauthorized** debe borrar el token y redirigir automáticamente a `/login`.

2. **Gestión de ficheros**

   * RF2.1: Subir CSV (`POST /files/upload`) con `multipart/form-data`.
   * RF2.2: Listar configuraciones (`GET /files/`), mostrando ID, file\_name, path, has\_header, delimiter, etc.
   * RF2.3: Enviar fichero a HDFS para validación (`POST /files/push/{file_name}`), mostrando “Enviando…” mientras dura la petición.
   * RF2.4: Editar metadatos de configuración (`PATCH /files/{id}`) desde un modal, mostrando “Guardando…” durante la petición.
   * RF2.5: Eliminar configuración (`DELETE /files/{id}`), con confirmación `window.confirm()` y botón “Eliminando…”.
   * RF2.6: Descargar fichero original o validado (`GET /files/download/{file_name}`) forzando descarga con `fetch()` + Blob.

3. **Visualización de logs**

   * RF3.1: Listar logs de validación (`GET /files/logs`), mostrando ID, file\_config\_id, file\_name, field\_name, environment, validation\_flag, error\_message, logged\_at.
   * RF3.2: La fecha debe formatearse en zona Madrid como `DD/MM/YYYY` (ignorar hora si la BBDD solo almacena fecha).

4. **UX/UI**

   * RF4.1: Formularios de login/register con validación de campos requeridos.
   * RF4.2: Mostrar estados de carga en botones (“Subiendo…”, “Enviando…”, “Guardando…”, “Eliminando…”).
   * RF4.3: Mensajes de alerta (`alert()`) para notificar éxito o error.
   * RF4.4: Modal que se cierra al hacer clic fuera o en “Guardar”.
   * RF4.5: Navbar con enlaces en negrita (**Dashboard**, **Logs**) y botón **Logout** a la derecha en color naranja.

---

### Requisitos no funcionales

* **RNF1: Seguridad**

  * El token JWT se almacena en `localStorage`.
  * Axios inyecta el token en encabezados de cada petición protegida.
  * El interceptor de respuesta detecta **401**, borra el token y redirige a `/login`.

* **RNF2: Rendimiento**

  * La carga inicial debe ser menor de 2 s en producción básica.
  * Peticiones asíncronas con indicadores de carga.

* **RNF3: Mantenibilidad**

  * Estructura modular: carpetas por funcionalidad (`api/`, `components/`, `contexts/`, `layouts/`, `pages/`, `routes/`, `styles/`).
  * Context API para centralizar la lógica de autenticación.

* **RNF4: Accesibilidad**

  * Cada `<input>` debe tener su correspondiente `<label>`.
  * Enlaces de navegación en negrita para mejorar legibilidad.
  * Contraste adecuado (botón naranja sobre fondo claro).

* **RNF5: Compatibilidad**

  * Navegadores modernos: Chrome, Firefox, Edge.
  * No se requiere soporte para Internet Explorer.

---

## 3. Diseño (Diagramas)

### Casos de uso

* **Registrar Usuario**

  1. Actor: usuario no autenticado.
  2. Accede a `/register`.
  3. Completa email y contraseña.
  4. Pulsa **Registrar** → `POST /auth/register`.
  5. Si la respuesta es 201, se redirige a `/login`.
  6. En caso de error, mostrar mensaje en formulario.
     **UML: Diagrama de casos de uso “Registrar Usuario”**

* **Iniciar Sesión**

  1. Actor: usuario no autenticado.
  2. Accede a `/login`.
  3. Completa email y contraseña.
  4. Pulsa **Entrar** → `POST /auth/login`.
  5. Si la respuesta es 200, guarda token en `localStorage` y redirige a `/dashboard`.
  6. Si error 401, mostrar mensaje de credenciales inválidas.
     **UML: Diagrama de casos de uso “Iniciar Sesión”**

* **Cerrar Sesión**

  1. Actor: usuario autenticado.
  2. En navbar, pulsa botón **Logout** (derecha).
  3. Context API borra token de `localStorage`.
  4. Redirige a `/login`.
     **UML: Diagrama de casos de uso “Cerrar Sesión”**

* **Subir Fichero**

  1. Actor: usuario autenticado.
  2. En `/dashboard`, selecciona un archivo CSV.
  3. Pulsa **Subir Fichero** → `POST /files/upload` con `multipart/form-data`.
  4. Mientras, botón muestra **Subiendo…**.
  5. Respuesta 201 con `{ file_config_id }` → alerta **Fichero subido ID: X** y refrescar lista (`GET /files/`).
  6. Si 401, interceptor borra token y redirige a `/login`.
     **UML: Diagrama de casos de uso “Subir Fichero”**

* **Listar Configuraciones**

  1. Actor: usuario autenticado.
  2. Al acceder a `/dashboard`, se ejecuta `GET /files/`.
  3. Mostrar tabla con cada configuración (ID, file\_name, path, has\_header, delimiter).
  4. Si el usuario pulsa **Detalles**, abrir modal; si pulsa **Validar** o **Eliminar**, ejecutar la acción correspondiente.
  5. Si 401, interceptor borra token y redirige a `/login`.
     **UML: Diagrama de casos de uso “Listar Configuraciones”**

* **Validar Fichero**

  1. Actor: usuario autenticado.
  2. Pulsa **Validar** en fila → botón **Enviando…**.
  3. Ejecuta `POST /files/push/{file_name}`.
  4. Si 200, alerta **Enviado a validar** y refresca lista.
  5. Si 401, interceptor borra token y redirige a `/login`.
     **UML: Diagrama de casos de uso “Validar Fichero”**

* **Editar Configuración (Modal)**

  1. Actor: usuario autenticado.
  2. Pulsa **Detalles** en fila → abre modal con campos prellenados.
  3. Modifica campos (delimiter, quote\_char, escape\_char, has\_header, date\_format, timestamp\_format, partition\_columns).
  4. Pulsa **Guardar** → botón **Guardando…**.
  5. Ejecuta `PATCH /files/{id}` con JSON de cambios.
  6. Si 200, alerta **Configuración actualizada**, cerrar modal y refrescar lista.
  7. Si 401, interceptor borra token y redirige a `/login`.
     **UML: Diagrama de casos de uso “Editar Configuración”**

* **Eliminar Configuración**

  1. Actor: usuario autenticado.
  2. Pulsa **Eliminar** (fila o modal) → `window.confirm("¿Estás seguro?")`.
  3. Si acepta, botón **Eliminando…**, ejecutar `DELETE /files/{id}`.
  4. Si 204, alerta **Configuración eliminada** y refrescar lista.
  5. Si 401, interceptor borra token y redirige a `/login`.
     **UML: Diagrama de casos de uso “Eliminar Configuración”**

* **Descargar Fichero**

  1. Actor: usuario autenticado.
  2. Pulsa **Descargar** (modal) → ejecutar

     ```js
     fetch(downloadFileUrl(file_name), {
       headers: { Authorization: `Bearer ${token}` }
     })
     ```
  3. Recibe Blob → crea `<a>` con `URL.createObjectURL(blob)` y `download=file_name` → `a.click()` → descarga.
  4. Si 401, interceptor borra token y redirige a `/login`.
     **UML: Diagrama de casos de uso “Descargar Fichero”**

* **Listar Logs de Validación**

  1. Actor: usuario autenticado.
  2. Accede a `/logs` → ejecutar `GET /files/logs`.
  3. Recibe array de logs → formatear `logged_at` con

     ```js
     new Date(l.logged_at).toLocaleDateString('es-ES')
     ```

     → mostrar tabla con columnas: ID, file\_config\_id, file\_name, field\_name, environment, validation\_flag, error\_message, fecha.
  4. Si 401, interceptor borra token y redirige a `/login`.
     **UML: Diagrama de casos de uso “Listar Logs”**

---

<!-- ### Diagrama entidad-relación (BBDD relacional)

* **users**:

  * id (PK)
  * email
  * password\_hash
  * is\_active
  * created\_at
* **file\_configs**:

  * id (PK)
  * file\_name
  * path
  * file\_format
  * has\_header (boolean)
  * delimiter
  * quote\_char
  * escape\_char
  * date\_format
  * timestamp\_format
  * partition\_columns
  * created\_by (FK → users.id)
  * created\_at
* **validation\_logs**:

  * id (PK)
  * file\_config\_id (FK → file\_configs.id)
  * file\_name
  * field\_name
  * environment
  * validation\_flag
  * error\_message
  * logged\_at (TIMESTAMP)

Relaciones:

* Un usuario tiene muchas configuraciones (`file_configs`).
* Cada configuración genera muchos logs (`validation_logs`).
  **UML: Diagrama ER “users–file\_configs–validation\_logs”**

--- -->

### Diagrama de clases del modelo (frontend)

* **AuthContext**

  * Propiedades:

    * `user` (objeto o null)
    * `token` (string o null)
  * Métodos:

    * `login(email, password)`
    * `register(email, password)`
    * `logout()`
* **FileConfig**

  * Propiedades:

    * `id`
    * `fileName`
    * `path`
    * `hasHeader`
    * `delimiter`
    * `quoteChar`
    * `escapeChar`
    * `dateFormat`
    * `timestampFormat`
    * `partitionColumns`
  * Métodos:

    * `fetchAll()`
    * `create()`
    * `update()`
    * `delete()`
    * `pushToHDFS()`
    * `download()`
* **ValidationLog**

  * Propiedades:

    * `id`
    * `fileConfigId`
    * `fileName`
    * `fieldName`
    * `environment`
    * `validationFlag`
    * `errorMessage`
    * `loggedAt`
  * Métodos:

    * `fetchAll()`
* **Componentes principales**

  * `AppRouter` (gestiona rutas y protege con `RequireAuth`)
  * `MainLayout` (navbar + `<Outlet/>`)
  * `Dashboard` (lista de configuraciones + lógica de acciones)
  * `FileDetailModal` (modal de edición/validación/eliminación/descarga)
  * `LogsPage` (muestra logs con fecha formateada)
    **UML: Diagrama de clases “AuthContext, FileConfig, ValidationLog, Dashboard, FileDetailModal, LogsPage”**

---

### Diagramas de secuencia

1. **Subida de Fichero**

   * Usuario → Dashboard (selecciona CSV) → Dashboard → `uploadFile(formData)` → Axios `POST /files/upload` → Backend responde `{ file_config_id: X }` → Dashboard muestra alerta → Dashboard llama `fetchConfigs()`.
     **UML: Secuencia “Subir archivo”**

2. **Validar Fichero**

   * Usuario → Dashboard (pulsa **Validar**) → Dashboard → `pushToHDFS(file_name)` → Axios `POST /files/push/{file_name}` → Backend responde 200 → Dashboard alerta “Enviado a validar” → Dashboard llama `fetchConfigs()`.
     **UML: Secuencia “Validar archivo”**

3. **Editar Configuración (Modal)**

   * Usuario → Dashboard (pulsa **Detalles**) → Modal abre con datos prellenados.
   * Usuario → Modal (modifica campos) → Modal → `updateConfig(id, formData)` → Axios `PATCH /files/{id}` → Backend responde objeto actualizado → Modal alerta “Configuración actualizada” → Modal cierra → Dashboard llama `fetchConfigs()`.
     **UML: Secuencia “Editar configuración”**

4. **Eliminar Configuración**

   * Usuario → Dashboard o Modal (pulsa **Eliminar**) → `window.confirm("¿Estás seguro?")` → Modal/Dashboard → `deleteConfig(id)` → Axios `DELETE /files/{id}` → Backend responde 204 → Modal/Dashboard alerta “Configuración eliminada” → Dashboard llama `fetchConfigs()`.
     **UML: Secuencia “Eliminar configuración”**

5. **Descargar Fichero**

   * Usuario → Modal (pulsa **Descargar**) → Modal → `fetch(downloadFileUrl(file_name), { headers: { Authorization: `Bearer \${token}` } })` → recibe Blob → crea `<a>` con `URL.createObjectURL(blob)` y `download=file_name` → `a.click()` → descarga.
     **UML: Secuencia “Descargar archivo”**

6. **Listar Logs**

   * Usuario → LogsPage (al montar) → `getLogs()` → Axios `GET /files/logs` → Backend responde array → LogsPage formatea fecha con `toLocaleDateString('es-ES')` → muestra tabla.
     **UML: Secuencia “Listar logs”**

---

## 4. Implementación (GIT)

### Diagrama de arquitectura

El frontend en React se comunica con el backend en FastAPI a través de API REST.

* **Cliente (React + CSS)**
* **Enrutado (React Router v6)**
* **Estado global (Context API – AuthContext)**
* **Comunicación HTTP (Axios con interceptores request/response)**
* **Bundler/Dev Server (Vite)**
  **UML: Diagrama de arquitectura “Frontend React con AuthContext y Axios”**

---

### Tecnologías

* React 18+
* React Router v6
* Axios (con interceptores de token y 401)
* Context API (AuthContext)
* CSS puro en `src/styles/index.css`
* Vite

---

### Código (Explicación de las partes más interesantes)

1. **`src/api/axiosConfig.js`**

   * Se crea una instancia de Axios con `baseURL: 'http://localhost:8000'`.
   * Interceptor de petición:

     ```js
     api.interceptors.request.use(config => {
       const token = localStorage.getItem('access_token');
       if (token) config.headers.Authorization = `Bearer ${token}`;
       return config;
     });
     ```
   * Interceptor de respuesta:

     ```js
     api.interceptors.response.use(
       response => response,
       error => {
         if (error.response && error.response.status === 401) {
           localStorage.removeItem('access_token');
           window.location.href = '/login';
           alert('Tu sesión ha expirado. Por favor, inicia sesión de nuevo.');
         }
         return Promise.reject(error);
       }
     );
     ```

2. **`src/contexts/AuthContext.jsx`**

   * Se define `AuthContext` y `AuthProvider`.
   * Se utiliza `useState` para `token` y `user`.
   * `useEffect` sincroniza `token` con `localStorage`.
   * Métodos:

     ```js
     const login = async (email, password) => {
       const res = await api.post('/auth/login', { email, password });
       setToken(res.data.access_token);
       navigate('/dashboard');
     };
     const register = async (email, password) => {
       await api.post('/auth/register', { email, password });
       navigate('/login');
     };
     const logout = () => {
       setToken(null);
       navigate('/login');
     };
     ```

3. **`src/routes/AppRouter.jsx`**

   * Se define `RequireAuth` usando `useLocation()` y `<Navigate>`.
   * Rutas públicas:

     ```jsx
     <Route path="/login" element={<LoginPage />} />
     <Route path="/register" element={<RegisterPage />} />
     ```
   * Rutas privadas anidadas bajo `/`:

     ```jsx
     <Route
       path="/"
       element={<RequireAuth><MainLayout/></RequireAuth>}
     >
       <Route index element={<Navigate to="/dashboard" replace />} />
       <Route path="dashboard" element={<Dashboard />} />
       <Route path="logs" element={<LogsPage />} />
     </Route>
     ```
   * Ruta comodín:

     ```jsx
     <Route path="*" element={<Navigate to="/login" replace />} />
     ```

4. **`src/layouts/MainLayout.jsx`**

   * Navbar con enlaces y botón Logout:

     ```jsx
     const navStyle = {
       display: 'flex',
       justifyContent: 'space-between',
       padding: '1rem',
       borderBottom: '1px solid #ccc'
     };
     const linkStyle = {
       marginRight: '1rem',
       textDecoration: 'none',
       color: '#333',
       fontWeight: 'bold'
     };
     const logoutButtonStyle = {
       background: '#e67e22',
       color: '#fff',
       padding: '0.5rem 1rem',
       borderRadius: '4px',
       border: 'none',
       cursor: 'pointer'
     };
     ```
   * `<Outlet/>` para inyectar rutas hijas:

     ```jsx
     <main style={{ padding: '1rem' }}>
       <Outlet />
     </main>
     ```

5. **`src/pages/Dashboard.jsx`**

   * `useEffect` para obtener configuraciones:

     ```jsx
     const fetchConfigs = async () => {
       const res = await getConfigs();
       setConfigs(res.data);
     };
     useEffect(() => { fetchConfigs(); }, []);
     ```
   * Subida de fichero:

     ```jsx
     const handleUpload = async () => {
       setUploading(true);
       const form = new FormData();
       form.append('file', selectedFile);
       const res = await uploadFile(form);
       alert(`Fichero subido ID: ${res.data.file_config_id}`);
       fetchConfigs();
       setUploading(false);
     };
     ```
   * Tabla con acciones por fila: “Validar” (texto “Enviando…”), “Eliminar” (texto “Eliminando…”), “Detalles”.
   * “Detalles” abre `<FileDetailModal>` con props `config`, `onClose`, `onRefresh`.

6. **`src/components/FileDetailModal.jsx`**

   * Overlay (`position: fixed`) + modal (`position: relative`).
   * Campos controlados para cada metadato (selects, checkbox, input).
   * Botones:

     ```jsx
     <button onClick={save} disabled={saving}>
       {saving ? 'Guardando...' : 'Guardar'}
     </button>
     <button onClick={validate} disabled={validating}>
       {validating ? 'Enviando…' : 'Validar'}
     </button>
     <button onClick={downloadFile}>
       Descargar
     </button>
     <button onClick={remove} disabled={deleting}>
       {deleting ? 'Eliminando…' : 'Eliminar'}
     </button>
     ```
   * `downloadFile` usa `fetch()` y Blob para forzar descarga.
   * Modal se cierra al hacer clic fuera (`onClick={onClose}` en overlay) y `e.stopPropagation()` dentro del modal para evitar el cierre.

7. **`src/pages/LogsPage.jsx`**

   * `useEffect` para `getLogs()`:

     ```jsx
     const fetchLogs = async () => {
       const res = await getLogs();
       setLogs(res.data);
     };
     useEffect(() => { fetchLogs(); }, []);
     ```
   * Mostrar tabla con logs y formatear fecha:

     ```jsx
     const fecha = new Date(l.logged_at).toLocaleDateString('es-ES');
     ```

8. **`src/main.jsx`**

   * React 18:

     ```jsx
     import { createRoot } from 'react-dom/client';
     const container = document.getElementById('root');
     const root = createRoot(container);
     root.render(
       <React.StrictMode>
         <BrowserRouter>
           <AuthProvider>
             <AppRouter />
           </AuthProvider>
         </BrowserRouter>
       </React.StrictMode>
     );
     ```

---

### Organización del proyecto. Patrón

```
frontend/
├── public/
│   └── index.html
├── src/
│   ├── api/
│   │   ├── axiosConfig.js
│   │   ├── auth.js
│   │   ├── files.js
│   │   └── logs.js
│   ├── components/
│   │   └── FileDetailModal.jsx
│   ├── contexts/
│   │   └── AuthContext.jsx
│   ├── layouts/
│   │   └── MainLayout.jsx
│   ├── pages/
│   │   ├── Dashboard.jsx
│   │   ├── LoginPage.jsx
│   │   ├── RegisterPage.jsx
│   │   └── LogsPage.jsx
│   ├── routes/
│   │   └── AppRouter.jsx
│   ├── styles/
│   │   └── index.css
│   └── main.jsx
├── .gitignore
├── package.json
└── README.md
```

* Patrón **feature-based**: cada carpeta agrupa archivos por responsabilidad (API, componentes, contexto, layouts, páginas, rutas, estilos).

---

## 5. Resultado (Manual de usuario)

### Requisitos previos

* Node.js v16+ y npm instalados.
* Backend ejecutándose en `http://localhost:8000`.

### Instalación y arranque

1. Clonar repositorio y entrar en carpeta:

   ```bash
   git clone https://github.com/tu-usuario/tu-repo-frontend.git
   cd tu-repo-frontend
   ```
2. Instalar dependencias:

   ```bash
   npm install
   ```
3. Levantar servidor de desarrollo:

   ```bash
   npm run dev
   ```
4. Abrir navegador en `http://localhost:5173`.

### Flujo de usuario

1. **Registro / Login**

   * Ir a `/register`, completar email y contraseña, pulsar **Registrar**.
   * Se redirige a `/login`; introducir credenciales y pulsar **Entrar**.
   * Si son válidas, se guarda el token y aparece la navbar con enlaces en negrita (**Dashboard**, **Logs**) y botón **Logout** (naranja) a la derecha.
   * Si el token falta o caduca, cualquier petición protegida mostrará un

     ```
     alert("Tu sesión ha expirado. Por favor, inicia sesión de nuevo.")
     ```

     y redirigirá a `/login`.

2. **Dashboard**

   * Seleccionar un archivo CSV en el input y pulsar **Subir Fichero** (se muestra **Subiendo…**).
   * En la tabla se listan todas las configuraciones con columnas: ID, Nombre de fichero, Path, Has Header, Delimiter.
   * Botones por fila:

     * **Validar**: al pulsar, muestra **Enviando…** y lanza `POST /files/push/{file_name}` → alerta de éxito → refrescar lista.
     * **Eliminar**: al pulsar, pide confirmación; si confirma, muestra **Eliminando…** y ejecuta `DELETE /files/{id}` → alerta de éxito → refrescar lista.

       * Si responde 401, interceptor borra token y redirige a `/login`.
     * **Detalles**: abre un modal con los metadatos.

3. **Modal de Detalles**

   * Campos editables:

     * Delimiter (Comma, Semicolon, Tab, Pipe)
     * Quote Character (Double, Single, None)
     * Escape Character (Backslash, Double, None)
     * Has Header (checkbox)
     * Date Format (YYYY-MM-DD, DD/MM/YYYY, MM-DD-YYYY)
     * Timestamp Format (YYYY-MM-DD HH\:mm\:ss, ISO 8601, Timestamp MS)
     * Partition Columns (texto libre)
   * Botones:

     * **Guardar**: muestra **Guardando…** y ejecuta `PATCH /files/{id}`.

       * Si responde 401, interceptor borra token y redirige a `/login`.
     * **Validar**: muestra **Enviando…** y ejecuta `POST /files/push/{file_name}`.

       * Si responde 401, interceptor borra token y redirige a `/login`.
     * **Descargar**: ejecuta `fetch()` con token y Blob para forzar descarga.

       * Si responde 401, interceptor borra token y redirige a `/login`.
     * **Eliminar**: muestra **Eliminando…** y ejecuta `DELETE /files/{id}` tras confirmación.

       * Si responde 401, interceptor borra token y redirige a `/login`.
   * El modal se cierra al hacer clic fuera o en **×**.

4. **Logs**

   * Acceder a `/logs` para ver la tabla de resultados de validación.
   * Columnas: ID, Config ID, Fichero, Campo, Entorno, Flag, Mensaje, Fecha (DD/MM/YYYY).
   * Si retorna 401, interceptor borra token y redirige a `/login`.

5. **Cerrar Sesión**

   * Pulsar **Logout** (botón naranja en la barra) borra token y redirige a `/login`.

---

## 6. Conclusiones

### Dificultades

* Gestionar **401 Unauthorized** en cualquier petición: se añadió un interceptor de respuesta en Axios que borra el token y redirige a `/login`.
* Proteger rutas privadas con React Router v6: se implementó `RequireAuth` usando `useLocation()` y `<Navigate>` para forzar redirección a `/login` cuando no hay token.
* Forzar la descarga de archivos protegidos: usar `fetch()` + Blob para crear un enlace `<a>` dinámico en lugar de `<a download>` tradicional.

### Mejoras

1. Implementar paginación en las listas de configuraciones y logs.
2. Añadir filtros avanzados en Logs (por fecha, entorno, validation\_flag).
3. Integrar un framework CSS (Tailwind, Material UI) para un diseño más consistente y responsivo.
4. Permitir subida masiva de archivos con procesamiento en paralelo.
5. Incorporar notificaciones en tiempo real (WebSocket) para el estado de validación.
6. Añadir internacionalización (i18n) para varios idiomas.
