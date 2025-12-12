$ErrorActionPreference = "Stop"

# Paths
$PROJECT_ROOT = "C:\Users\A&A COMPUTER\eclipse-workspace\ClearanceManagementSystem"
$SRC = "$PROJECT_ROOT\src"
$OUT = "$PROJECT_ROOT\out_trae"
$LIB_JAVAFX = "C:\Program Files\Java\javafx-sdk-21.0.8\lib"
$LIB_MYSQL = "C:\Program Files\MySQL\mysql-connector-j-9.3.0\mysql-connector-j-9.3.0.jar"
$LIB_PDFBOX = "C:\Program Files\Java\pdfbox-app-3.0.6.jar"
$LIB_FONTBOX = "C:\Program Files\Java\fontbox-3.0.6.jar"
$LIB_ITEXT = "C:\Program Files\Java\PDF itex 7\*"
$LIB_SLF4J = "C:\Program Files\Java\sif4j\*"

# Clean and Create Output Directory
if (Test-Path $OUT) { Remove-Item -Recurse -Force $OUT }
New-Item -ItemType Directory -Force -Path $OUT | Out-Null

# Classpath for Compilation
$CLASSPATH = "$LIB_JAVAFX\*;$LIB_MYSQL;$LIB_PDFBOX;$LIB_FONTBOX;$LIB_ITEXT;$LIB_SLF4J"

Write-Host "Compiling Java sources..."
# Get all java files
$javaFiles = Get-ChildItem -Path $SRC -Recurse -Filter "*.java" | Select-Object -ExpandProperty FullName
# Save list to file for javac, quoting paths with spaces and using forward slashes
$javaFiles | ForEach-Object { "`"$($_ -replace '\\', '/')`"" } | Out-File "$PROJECT_ROOT\sources.txt" -Encoding ascii

# Compile
# Note: encoding utf8 might be needed if there are special chars
javac -encoding UTF-8 -d $OUT -cp $CLASSPATH --module-path $LIB_JAVAFX --add-modules javafx.controls,javafx.fxml "@$PROJECT_ROOT\sources.txt"

# Copy Resources (fxml, css, images) - preserving directory structure
Write-Host "Copying resources..."
# We need to copy resources from src/com/... to out_trae/com/...
# Copy-Item -Recurse is easiest but might copy .java files too, which is fine but messy.
# Let's just copy everything and then remove .java if we care (we don't).
Copy-Item "$SRC\*" "$OUT" -Recurse -Force

# Remove java files from output to keep it clean (optional)
Get-ChildItem -Path $OUT -Recurse -Filter "*.java" | Remove-Item

Write-Host "Running Application..."
java -cp "$OUT;$CLASSPATH" --module-path $LIB_JAVAFX --add-modules javafx.controls,javafx.fxml com.university.clearance.Main
