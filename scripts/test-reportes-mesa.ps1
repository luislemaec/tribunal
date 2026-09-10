$ErrorActionPreference = 'Stop'
$raizProyecto = Split-Path -Parent $PSScriptRoot
$pomTemporal = Join-Path $raizProyecto ('.pom-reportes-' + [guid]::NewGuid().ToString('N') + '.xml')
Push-Location $raizProyecto
try {
    # Surefire omite pruebas en el POM habitual. La copia conserva todas sus dependencias.
    $contenidoPom = Get-Content -Raw -LiteralPath (Join-Path $raizProyecto 'pom.xml')
    $contenidoPom = $contenidoPom.Replace('<skip>true</skip>', '<skip>false</skip>')
    $contenidoPom = $contenidoPom.Replace('<build>', '<build><directory>${project.basedir}/target/reportes-validation</directory>')
    [System.IO.File]::WriteAllText($pomTemporal, $contenidoPom, [System.Text.UTF8Encoding]::new($false))
    & mvn -f $pomTemporal '-Dtest=ReglasDocumentoMesaTest,RevisionActaFinalTest,AccesoDocumentoMesaTest,ConsultasDocumentoMesaTest,ComandosDocumentoMesaTest,ConformacionJuntaTest' test
    if ($LASTEXITCODE -ne 0) { throw 'Fallaron las pruebas de documentos de mesa.' }
} finally {
    if (Test-Path -LiteralPath $pomTemporal) { Remove-Item -LiteralPath $pomTemporal }
    Pop-Location
}
