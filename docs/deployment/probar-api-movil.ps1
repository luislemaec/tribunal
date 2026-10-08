# Prueba de la API móvil de TEC (docs/api-movil.md) con un usuario real.
# Pide la clave sin mostrarla ni guardarla, inicia sesión, consulta los endpoints del
# módulo Tribunal y cierra la sesión. No imprime la clave ni los tokens.
#
# Uso (desde cmd o PowerShell):
#   powershell -NoProfile -ExecutionPolicy Bypass -File docs\deployment\probar-api-movil.ps1 -Usuario USUARIO
#   ... -Base https://tribunal.conpociiech.org/api/v1   (otro entorno)
param(
    [Parameter(Mandatory = $true)][string]$Usuario,
    [string]$Base = "https://tribunal.local/api/v1"
)

[Console]::OutputEncoding = [Text.Encoding]::UTF8
[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12

function Mostrar-Error($e) {
    if ($e.ErrorDetails -and $e.ErrorDetails.Message) { return $e.ErrorDetails.Message }
    if ($e.Exception.Response) { return "HTTP $([int]$e.Exception.Response.StatusCode)" }
    return $e.Exception.Message
}

$segura = Read-Host "Clave de $Usuario" -AsSecureString
$bstr = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($segura)
try {
    $clave = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($bstr)
    $cuerpo = @{ usuario = $Usuario; clave = $clave; dispositivo = "prueba-ps1" } | ConvertTo-Json
} finally {
    [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($bstr)
    Remove-Variable clave -ErrorAction SilentlyContinue
}

try {
    $sesion = Invoke-RestMethod -Uri "$Base/auth/login" -Method Post -ContentType "application/json; charset=utf-8" `
        -Body ([Text.Encoding]::UTF8.GetBytes($cuerpo))
} catch {
    Write-Host "== login: $(Mostrar-Error $_)"
    exit 1
} finally {
    Remove-Variable cuerpo -ErrorAction SilentlyContinue
}

Write-Host "== login: OK  usuario=$($sesion.usuario.usuario)  roles=$($sesion.usuario.roles -join ', ')  cambioClaveObligatorio=$($sesion.cambioClaveObligatorio)"
$cabeceras = @{ Authorization = "Bearer $($sesion.accessToken)" }

foreach ($ruta in "auth/yo", "tribunal/presidente/mesa", "tribunal/presidente/padron", "tribunal/iglesia",
        "tribunal/iglesia/miembros?pagina=0&tamano=5", "tribunal/proceso/resumen", "tribunal/proceso/mesas",
        "tribunal/proceso/resultados") {
    try {
        $json = Invoke-RestMethod -Uri "$Base/$ruta" -Headers $cabeceras | ConvertTo-Json -Depth 6 -Compress
        if ($json.Length -gt 600) { $json = $json.Substring(0, 600) + " ...(recortado)" }
        Write-Host "== ${ruta}: OK $json"
    } catch {
        Write-Host "== ${ruta}: $(Mostrar-Error $_)"
    }
}

try {
    Invoke-RestMethod -Uri "$Base/auth/logout" -Method Post -Headers $cabeceras | Out-Null
    Write-Host "== logout: OK"
} catch {
    Write-Host "== logout: $(Mostrar-Error $_)"
}
