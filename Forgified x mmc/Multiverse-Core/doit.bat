@echo off
setlocal enabledelayedexpansion

set "input_file=worlds.yml"
set "temp_file=temp_worlds.yml"

if not exist "%input_file%" (
    echo File "%input_file%" does not exist.
    exit /b 1
)

> "%temp_file%" (
    for /f "usebackq delims=" %%A in ("%input_file%") do (
        set "line=%%A"
        rem Remove leading spaces or tabs
        set "trimmed=!line!"
        if "!trimmed:    icon=!" neq "!trimmed!" (
            echo     icon: EMPTY_MAP
        ) else if "!trimmed:    icon-data=!" neq "!trimmed!" (
            echo     icon-data: 0
        ) else if "!trimmed:    environment=!" neq "!trimmed!" (
            echo     environment: NORMAL
        ) else if "!trimmed:    generator=!" neq "!trimmed!" (
            echo     generator: VoidGen
        ) else (
            echo !line!
        )
    )
)

move /Y "%temp_file%" "%input_file%"
echo Replacement complete.
