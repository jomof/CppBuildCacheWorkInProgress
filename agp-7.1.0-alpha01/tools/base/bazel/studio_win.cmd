@rem Invoked by Android Build Launchcontrol for continuous builds.
@rem Windows Android Studio Remote Bazel Execution Script.
setlocal enabledelayedexpansion
set PATH=c:\tools\msys64\usr\bin;%PATH%
@rem Expected arguments:
set OUTDIR=%1
set DISTDIR=%2
set BUILDNUMBER=%3
@rem DETECT_FLAKES is an optional argument adding extra bazel arguments.
set DETECT_FLAKES=%4

@REM It is a post-submit build if the build number does not start with "P"
IF "%BUILDNUMBER:~0,1%"=="P" (
  SET /A IS_POST_SUBMIT=0
) ELSE (
  SET /A IS_POST_SUBMIT=1
)

@REM Run tests multiple times to aid flake detection.
IF "%DETECT_FLAKES%"=="--detect_flakes" (
  SET ATTEMPTS=--flaky_test_attempts=3
  set NOCACHE=--nocache_test_results
  set CONDITIONAL_FLAGS=!ATTEMPTS! !NOCACHE!
) ELSE IF %IS_POST_SUBMIT% EQU 1 (
  SET NOCACHE=--nocache_test_results
  SET FLAKY_ATTEMPTS=--flaky_test_attempts=2
  SET CONDITIONAL_FLAGS=!NOCACHE! !FLAKY_ATTEMPTS!
)

set TESTTAGFILTERS=-no_windows,-no_test_windows,-qa_smoke,-qa_fast,-qa_unreliable,-perfgate

@rem The current directory the executing script is in.
set SCRIPTDIR=%~dp0
CALL :NORMALIZE_PATH "%SCRIPTDIR%..\..\.."
set BASEDIR=%RETVAL%

@rem Generate a UUID for use as the Bazel invocation ID
FOR /F "tokens=*" %%F IN ('uuidgen') DO (
  SET INVOCATIONID=%%F
)

echo "Called with the following:  OUTDIR=%OUTDIR%, DISTDIR=%DISTDIR%, BUILDNUMBER=%BUILDNUMBER%, SCRIPTDIR=%SCRIPTDIR%, BASEDIR=%BASEDIR%"

set TARGETS=
for /f %%i in (%SCRIPTDIR%targets.win) do set TARGETS=!TARGETS! %%i

@echo studio_win.cmd time: %time%
@rem Run Bazel
CALL %SCRIPTDIR%bazel.cmd ^
 --max_idle_secs=60 ^
 test ^
 --keep_going ^
 --config=dynamic ^
 --build_tag_filters=-no_windows ^
 --invocation_id=%INVOCATIONID% ^
 --build_event_binary_file=%DISTDIR%\bazel-%BUILDNUMBER%.bes ^
 --test_tag_filters=%TESTTAGFILTERS% ^
 --profile=%DISTDIR%\winprof%BUILDNUMBER%.json.gz ^
 %CONDITIONAL_FLAGS% ^
 -- ^
 //tools/base/profiler/native/trace_processor_daemon ^
 %TARGETS%

SET EXITCODE=%errorlevel%
@echo studio_win.cmd time: %time%

IF NOT EXIST %DISTDIR%\ GOTO ENDSCRIPT

echo "<meta http-equiv="refresh" content="0; URL='https://source.cloud.google.com/results/invocations/%INVOCATIONID%'" />" > %DISTDIR%\upsalite_test_results.html
@echo studio_win.cmd time: %time%

@rem copy skia parser artifact to dist dir
copy %BASEDIR%\bazel-bin\tools\base\dynamic-layout-inspector\skia\skiaparser.zip %DISTDIR%

@rem copy trace processor daemon artifact to dist dir
copy %BASEDIR%\bazel-bin\tools\base\profiler\native\trace_processor_daemon\trace_processor_daemon.exe %DISTDIR%

@echo studio_win.cmd time: %time%

IF %IS_POST_SUBMIT% EQU 1 (
  SET PERFGATE_ARG=-perfzip %DISTDIR%\perfgate_data.zip
) ELSE (
  SET PERFGATE_ARG=
)


CALL %SCRIPTDIR%bazel.cmd ^
 --max_idle_secs=60 ^
 run //tools/vendor/adt_infra_internal/rbe/logscollector:logs-collector ^
 --config=dynamic ^
 -- ^
 -bes %DISTDIR%\bazel-%BUILDNUMBER%.bes ^
 -testlogs %DISTDIR%\logs\junit ^
 %PERFGATE_ARG%

IF ERRORLEVEL 1 (
  @echo Bazel logs-collector failed
  IF %IS_POST_SUBMIT% EQU 1 (
    SET EXITCODE=1
  )
  GOTO ENDSCRIPT
)

@echo studio_win.cmd time: %time%

@rem Extra debugging for b/162585987
dir %EXECROOT%\bazel-out\host\bin\tools\base\bazel\kotlinc.exe.runfiles
dir %EXECROOT%\bazel-out\host\bin\tools\base\bazel\formc.exe.runfiles

:ENDSCRIPT
@rem On windows we must explicitly shut down bazel. Otherwise file handles remain open.
@echo studio_win.cmd time: %time%
CALL %SCRIPTDIR%bazel.cmd shutdown
@echo studio_win.cmd time: %time%
@rem We also must call the kill-processes.py python script and kill all processes still open
@rem within the src directory.  This is due to the fact go/ab builds must be removable after
@rem execution, and any open processes will prevent this removal on windows.
CALL %BASEDIR%\tools\vendor\adt_infra_internal\build\scripts\slave\kill-processes.cmd %BASEDIR%
@echo studio_win.cmd time: %time%

SET /A BAZEL_EXITCODE_TEST_FAILURES=3

IF %IS_POST_SUBMIT% EQU 1 (
  IF %EXITCODE% EQU %BAZEL_EXITCODE_TEST_FAILURES% (
    EXIT /B 0
  )
)
EXIT /B %EXITCODE%

@rem HELPER FUNCTIONS
:NORMALIZE_PATH
  SET RETVAL=%~dpfn1
  EXIT /B
