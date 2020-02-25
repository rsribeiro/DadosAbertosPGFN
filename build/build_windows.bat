REM Para criar o executável nativo .exe é necessário ter a GraalVM 11 com o módulo Native Image instalado e também das ferramentas de build do Visual Studio
call "C:\Program Files (x86)\Microsoft Visual Studio\2019\Community\VC\Auxiliary\Build\vcvars64.bat"
call mvn package -f "../pgfn"
pause
