@echo off
call "C:\Program Files (x86)\Microsoft Visual Studio\2019\Community\VC\Auxiliary\Build\vcvars64.bat"
call mvn package -f "..\pgfn"
call "c:\Tools\upx-3.96-win64\upx.exe" "..\pgfn\target\JuntaArquivosPGFN.exe" --best --lzma
pause
