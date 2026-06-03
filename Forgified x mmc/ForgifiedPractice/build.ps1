$ErrorActionPreference = "Stop"
$MavenVersion = "3.9.6"
$MavenUrl = "https://archive.apache.org/dist/maven/maven-3/$MavenVersion/binaries/apache-maven-$MavenVersion-bin.zip"
$MavenZip = "$env:TEMP\maven.zip"
$MavenDir = "$env:TEMP\maven"

if (-not (Test-Path "$MavenDir\apache-maven-$MavenVersion\bin\mvn.cmd")) {
    Write-Host "Downloading Maven $MavenVersion..."
    Invoke-WebRequest -Uri $MavenUrl -OutFile $MavenZip
    Write-Host "Extracting Maven..."
    Expand-Archive -Path $MavenZip -DestinationPath $MavenDir -Force
}

if (-not $env:JAVA_HOME -or -not (Test-Path "$env:JAVA_HOME\bin\javac.exe")) {
    $PotentialJdk = "C:\Program Files\JetBrains\IntelliJ IDEA 2025.3.1\jbr"
    if (Test-Path "$PotentialJdk\bin\javac.exe") {
        $env:JAVA_HOME = $PotentialJdk
        Write-Host "Using JDK from IntelliJ: $env:JAVA_HOME"
    }
}

$MvnCmd = "$MavenDir\apache-maven-$MavenVersion\bin\mvn.cmd"
Write-Host "Building ForgifiedPractice with Maven..."
& $MvnCmd clean package -U

if ($LASTEXITCODE -eq 0) {
    $jar = Get-Item "target\ForgifiedPractice*.jar" -ErrorAction SilentlyContinue | Select-Object -First 1
    if ($jar) {
        Copy-Item $jar.FullName -Destination "forgifiedpractice.jar" -Force
        Write-Host "Copied $($jar.Name) -> forgifiedpractice.jar"
    }
    Write-Host "Build successful!"
} else {
    Write-Host "Build failed."
}
