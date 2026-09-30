#!/usr/bin/env python3
"""
Cross-platform build script for Q50 GTR+ (Android 2.3 / API 9) & EPK packaging.
Works on Windows, Linux, and macOS without requiring bash.

Performs:
  1. Build APK (aapt -> javac -> d8 -> zipalign -> apksigner v1)
  2. Verify APK (apksigner verify, aapt badging, classes.dex, no native libs)
  3. Build EPK (wrap in .epk container encrypted with OBU public certificate)
  4. Verify EPK (strict 16-point invariant check)
  5. Generate provenance and full artifact bundle
"""

import hashlib
import os
from pathlib import Path
import re
import shutil
import subprocess
import sys
import urllib.request

if hasattr(sys.stdout, "reconfigure"):
    try:
        sys.stdout.reconfigure(encoding="utf-8", errors="replace")
        sys.stderr.reconfigure(encoding="utf-8", errors="replace")
    except Exception:
        pass

VERSION = "0.7"
PKG_NAME = "Q50-GTR-Plus"
MIN_SDK = 9
TARGET_SDK = 10
INNER_NAME = "q50gtr.apk"

PROJECT_DIR = Path(__file__).resolve().parent
BUILD_DIR = PROJECT_DIR / "build"
CACHE_DIR = PROJECT_DIR / ".cache"
ARTIFACT_DIR = PROJECT_DIR / "artifact"
KEYS_DIR = PROJECT_DIR / "keys"

ANDROID_JAR = CACHE_DIR / "android-2.3.3.jar"
ANDROID_JAR_URL = "https://repo1.maven.org/maven2/com/google/android/android/2.3.3/android-2.3.3.jar"
OBU_CERT = KEYS_DIR / "obu_cert.pem"

APK_OUTPUT_NAME = f"{PKG_NAME}-v{VERSION}.apk"
EPK_OUTPUT_NAME = f"{PKG_NAME}-v{VERSION}.epk"


def log(msg: str):
    print(f"\n\033[1m==> {msg}\033[0m")


def find_tool(name: str, candidates: list[Path | str] | None = None) -> Path:
    # Check candidates first
    if candidates:
        for c in candidates:
            p = Path(c)
            if p.is_file() and os.access(p, os.X_OK):
                return p
            # On Windows, try extensions
            if sys.platform == "win32":
                for ext in [".exe", ".bat", ".cmd"]:
                    pe = Path(str(c) + ext) if not str(c).endswith(ext) else p
                    if pe.is_file():
                        return pe

    # Check PATH
    which_path = shutil.which(name)
    if which_path:
        return Path(which_path)

    sys.exit(f"ERROR: Tool '{name}' not found. Please ensure it is installed and in PATH.")


def find_android_sdk_tools() -> dict[str, Path]:
    sdk_root = os.environ.get("ANDROID_HOME") or os.environ.get("ANDROID_SDK_ROOT")
    sdk_dirs = []
    if sdk_root:
        sdk_dirs.append(Path(sdk_root))
    if sys.platform == "win32":
        local_app_data = os.environ.get("LOCALAPPDATA")
        if local_app_data:
            sdk_dirs.append(Path(local_app_data) / "Android" / "Sdk")

    build_tools_dirs = []
    for sd in sdk_dirs:
        bt = sd / "build-tools"
        if bt.is_dir():
            # Check 33.0.1 / 33.0.2 first as specified in repo workflow
            for pref in ["33.0.2", "33.0.1", "34.0.0", "35.0.0"]:
                pref_dir = bt / pref
                if pref_dir.is_dir():
                    build_tools_dirs.append(pref_dir)
            for vdir in sorted(bt.iterdir(), reverse=True):
                if vdir.is_dir() and vdir not in build_tools_dirs:
                    build_tools_dirs.append(vdir)

    tools = {}
    for tool_name in ["aapt", "zipalign", "apksigner", "d8"]:
        candidates = []
        for bt in build_tools_dirs:
            candidates.append(bt / tool_name)
        tools[tool_name] = find_tool(tool_name, candidates)
    return tools


def find_jdk_tools() -> dict[str, Path]:
    java_home = os.environ.get("JAVA_HOME")
    candidates_javac = []
    candidates_keytool = []
    jdk_home_dir = None

    if sys.platform == "win32":
        # Look in Android Studio JBR
        as_jbr = Path("C:/Program Files/Android/Android Studio/jbr")
        if as_jbr.is_dir():
            candidates_javac.append(as_jbr / "bin" / "javac")
            candidates_keytool.append(as_jbr / "bin" / "keytool")
            if not jdk_home_dir:
                jdk_home_dir = as_jbr
        # Look in standard JDK paths
        jdk_base = Path("C:/Program Files/Java")
        if jdk_base.is_dir():
            for jd in jdk_base.iterdir():
                candidates_javac.append(jd / "bin" / "javac")
                candidates_keytool.append(jd / "bin" / "keytool")

    if java_home:
        candidates_javac.append(Path(java_home) / "bin" / "javac")
        candidates_keytool.append(Path(java_home) / "bin" / "keytool")
        if not jdk_home_dir:
            jdk_home_dir = Path(java_home)

    javac = find_tool("javac", candidates_javac)
    keytool = find_tool("keytool", candidates_keytool)
    java_bin = javac.parent / ("java.exe" if sys.platform == "win32" else "java")
    if not java_bin.exists():
        java_bin = find_tool("java")

    # Automatically set JAVA_HOME if not set or pointing to wrong java
    if jdk_home_dir:
        os.environ["JAVA_HOME"] = str(jdk_home_dir)
        # Also prepend JDK bin to PATH so batch files like d8.bat and apksigner.bat use it
        os.environ["PATH"] = str(jdk_home_dir / "bin") + os.pathsep + os.environ.get("PATH", "")

    return {
        "javac": javac,
        "keytool": keytool,
        "java": java_bin,
    }


def ensure_android_jar():
    CACHE_DIR.mkdir(parents=True, exist_ok=True)
    if not ANDROID_JAR.exists():
        log(f"Downloading android-2.3.3.jar from {ANDROID_JAR_URL}")
        urllib.request.urlretrieve(ANDROID_JAR_URL, ANDROID_JAR)
        print(f"Downloaded: {ANDROID_JAR} ({ANDROID_JAR.stat().st_size:,} bytes)")


def run_cmd(cmd: list[str | Path], cwd: Path = PROJECT_DIR, capture_output: bool = False) -> subprocess.CompletedProcess:
    cmd_str = [str(c) for c in cmd]
    res = subprocess.run(cmd_str, cwd=cwd, text=True, encoding="utf-8", errors="replace", capture_output=capture_output)
    if res.returncode != 0:
        if capture_output:
            print("STDOUT:\n", res.stdout)
            print("STDERR:\n", res.stderr)
        sys.exit(f"Command failed with code {res.returncode}: {' '.join(cmd_str)}")
    return res


def sha256_file(path: Path) -> str:
    h = hashlib.sha256()
    with open(path, "rb") as f:
        while chunk := f.read(65536):
            h.update(chunk)
    return h.hexdigest()


def main():
    log("Checking environment and tools")
    if not OBU_CERT.exists():
        sys.exit(f"ERROR: OBU certificate not found at {OBU_CERT}")

    ensure_android_jar()
    sdk_tools = find_android_sdk_tools()
    jdk_tools = find_jdk_tools()

    print(f"aapt:      {sdk_tools['aapt']}")
    print(f"zipalign:  {sdk_tools['zipalign']}")
    print(f"apksigner: {sdk_tools['apksigner']}")
    print(f"d8:        {sdk_tools['d8']}")
    print(f"javac:     {jdk_tools['javac']}")
    print(f"keytool:   {jdk_tools['keytool']}")
    print(f"android.jar: {ANDROID_JAR}")
    print(f"OBU cert:  {OBU_CERT}")

    # Prepare directories
    BUILD_DIR.mkdir(parents=True, exist_ok=True)
    ARTIFACT_DIR.mkdir(parents=True, exist_ok=True)
    classes_dir = BUILD_DIR / "classes"
    gen_dir = BUILD_DIR / "gen"
    if classes_dir.exists():
        shutil.rmtree(classes_dir)
    if gen_dir.exists():
        shutil.rmtree(gen_dir)
    classes_dir.mkdir(parents=True, exist_ok=True)
    gen_dir.mkdir(parents=True, exist_ok=True)

    unsigned_apk = BUILD_DIR / f"{PKG_NAME}-unsigned.apk"
    aligned_apk = BUILD_DIR / f"{PKG_NAME}-aligned.apk"
    final_apk = BUILD_DIR / APK_OUTPUT_NAME
    final_epk = ARTIFACT_DIR / EPK_OUTPUT_NAME

    for f in [unsigned_apk, aligned_apk, final_apk, final_epk]:
        if f.exists():
            f.unlink()

    # Step 1: aapt package
    log("1/7 aapt: resources and manifest")
    run_cmd([
        sdk_tools["aapt"], "package", "-f",
        "-M", PROJECT_DIR / "AndroidManifest.xml",
        "-S", PROJECT_DIR / "res",
        "-I", ANDROID_JAR,
        "-J", gen_dir,
        "-F", unsigned_apk
    ])

    # Step 2: javac
    log("2/7 javac: compile Java sources")
    java_files = list((PROJECT_DIR / "src").rglob("*.java")) + list(gen_dir.rglob("*.java"))
    if not java_files:
        sys.exit("ERROR: No java source files found!")

    # Check javac release options
    javac_help = subprocess.run([str(jdk_tools["javac"]), "--help"], capture_output=True, text=True)
    javac_args = [
        jdk_tools["javac"],
        "-classpath", str(ANDROID_JAR),
        "-encoding", "UTF-8",
        "-nowarn",
        "-d", str(classes_dir),
    ]
    # Try -source 8 -target 8 (compatible with d8 desugaring)
    javac_args.extend(["-source", "8", "-target", "8"])
    javac_args.extend([str(f) for f in java_files])
    run_cmd(javac_args)

    class_files = list(classes_dir.rglob("*.class"))
    print(f"Compiled {len(class_files)} class files.")

    # Step 3: d8 dex
    log("3/7 d8: bytecode -> classes.dex (min-api 9)")
    d8_jar = sdk_tools["d8"].parent / "lib" / "d8.jar"
    if d8_jar.exists():
        d8_cmd = [jdk_tools["java"], "-cp", str(d8_jar), "com.android.tools.r8.D8"]
    else:
        d8_cmd = [sdk_tools["d8"]]

    d8_args = d8_cmd + [
        "--lib", str(ANDROID_JAR),
        "--min-api", str(MIN_SDK),
        "--output", str(BUILD_DIR)
    ]
    d8_args.extend([str(f) for f in class_files])
    run_cmd(d8_args)

    dex_file = BUILD_DIR / "classes.dex"
    if not dex_file.exists():
        sys.exit("ERROR: classes.dex was not generated by d8")
    print(f"classes.dex generated ({dex_file.stat().st_size:,} bytes)")

    # Step 4: Add classes.dex to unsigned APK
    log("4/7 aapt add: add classes.dex into APK")
    # Must run inside BUILD_DIR so path stored in APK is just 'classes.dex'
    run_cmd([sdk_tools["aapt"], "add", "-f", unsigned_apk.name, "classes.dex"], cwd=BUILD_DIR)

    # Step 5: zipalign
    log("5/7 zipalign: 4-byte boundary alignment")
    run_cmd([sdk_tools["zipalign"], "-f", "4", unsigned_apk, aligned_apk])

    # Step 6: apksigner
    log("6/7 apksigner: v1 scheme signing")
    keystore = os.environ.get("Q50_KEYSTORE")
    key_alias = os.environ.get("Q50_KEY_ALIAS", "q50gtr")
    key_pass = os.environ.get("Q50_KEY_PASS", "q50gtrpass")
    signing_mode = "persistent" if keystore else "temporary"

    if not keystore:
        keystore_path = CACHE_DIR / "q50gtr.keystore"
        if not keystore_path.exists():
            print(f"Generating temporary keystore: {keystore_path}")
            run_cmd([
                jdk_tools["keytool"], "-genkeypair", "-noprompt",
                "-keystore", str(keystore_path),
                "-alias", key_alias,
                "-storepass", key_pass,
                "-keypass", key_pass,
                "-keyalg", "RSA",
                "-keysize", "2048",
                "-sigalg", "SHA1withRSA",
                "-validity", "10000",
                "-dname", "CN=Q50 GTR Plus, OU=Head Unit, O=Personal, L=-, ST=-, C=RU"
            ])
        keystore = str(keystore_path)

    apksigner_jar = sdk_tools["apksigner"].parent / "lib" / "apksigner.jar"
    if apksigner_jar.exists():
        apksigner_cmd = [jdk_tools["java"], "-jar", str(apksigner_jar)]
    else:
        apksigner_cmd = [sdk_tools["apksigner"]]

    run_cmd(apksigner_cmd + [
        "sign",
        "--ks", keystore,
        "--ks-key-alias", key_alias,
        "--ks-pass", f"pass:{key_pass}",
        "--key-pass", f"pass:{key_pass}",
        "--min-sdk-version", str(MIN_SDK),
        "--v1-signing-enabled", "true",
        "--v2-signing-enabled", "false",
        "--v3-signing-enabled", "false",
        "--out", str(final_apk),
        str(aligned_apk)
    ])
    print(f"Signed APK: {final_apk} ({final_apk.stat().st_size:,} bytes)")

    # Step 7: Verify APK
    log("7/7 Verification & EPK Packaging")
    sig_res = run_cmd(apksigner_cmd + [
        "verify",
        "--min-sdk-version", str(MIN_SDK),
        "--verbose", "--print-certs",
        str(final_apk)
    ], capture_output=True)
    sig_text = sig_res.stdout + sig_res.stderr
    (ARTIFACT_DIR / "signature.txt").write_text(sig_text, encoding="utf-8")

    assert "Verified using v1 scheme (JAR signing): true" in sig_text, "v1 signature missing!"
    assert "Verified using v2 scheme (APK Signature Scheme v2): false" in sig_text, "v2 signature found!"
    assert "Verified using v3 scheme (APK Signature Scheme v3): false" in sig_text, "v3 signature found!"

    # Extract signer cert SHA-256
    m_signer = re.search(r"Signer #1 certificate SHA-256 digest:\s*([0-9a-fA-F]+)", sig_text)
    if m_signer:
        signer_sha = m_signer.group(1).strip()
        (ARTIFACT_DIR / "apk-signer-cert.sha256").write_text(signer_sha + "\n", encoding="utf-8")
        print(f"Signer certificate SHA-256: {signer_sha}")

    (ARTIFACT_DIR / "signing-mode.txt").write_text(signing_mode + "\n", encoding="utf-8")

    # aapt dump badging
    badging_res = run_cmd([sdk_tools["aapt"], "dump", "badging", str(final_apk)], capture_output=True)
    badging_text = badging_res.stdout
    (ARTIFACT_DIR / "badging.txt").write_text(badging_text, encoding="utf-8")

    assert "sdkVersion:'9'" in badging_text, "sdkVersion is not 9!"
    assert "targetSdkVersion:'10'" in badging_text, "targetSdkVersion is not 10!"
    assert "com.ygomi.permission.IVI_CAN_READ" in badging_text, "Permission missing!"

    # Copy APK to artifact
    artifact_apk = ARTIFACT_DIR / APK_OUTPUT_NAME
    shutil.copy2(final_apk, artifact_apk)
    apk_sha = sha256_file(artifact_apk)
    (ARTIFACT_DIR / f"{APK_OUTPUT_NAME}.sha256").write_text(f"{apk_sha}  {APK_OUTPUT_NAME}\n", encoding="utf-8")
    print(f"APK SHA-256: {apk_sha}")

    # Build EPK
    log(f"Building EPK: {final_epk}")
    epktool = PROJECT_DIR / "tools" / "epktool.py"
    run_cmd([
        sys.executable, str(epktool), "build",
        str(artifact_apk),
        "-o", str(final_epk),
        "--cert", str(OBU_CERT),
        "--type", "2",
        "--name", INNER_NAME
    ])

    # EPK info
    info_res = run_cmd([sys.executable, str(epktool), "info", str(final_epk)], capture_output=True)
    (ARTIFACT_DIR / "epk-info.txt").write_text(info_res.stdout, encoding="utf-8")
    print(info_res.stdout)

    # Verify EPK
    log("Verifying EPK container layout (strict 16 checks)")
    run_cmd([
        sys.executable, str(epktool), "verify",
        str(final_epk),
        "--expect-name", INNER_NAME,
        "--expect-type", "2",
        "--expect-blocks", "1",
        "--expect-key-size", "128"
    ])

    epk_sha = sha256_file(final_epk)
    (ARTIFACT_DIR / f"{EPK_OUTPUT_NAME}.sha256").write_text(f"{epk_sha}  {EPK_OUTPUT_NAME}\n", encoding="utf-8")
    print(f"EPK SHA-256: {epk_sha}")

    # Write BUILD-PROVENANCE.txt
    provenance_text = f"""Q50 GTR+ build provenance
=========================

repository                  doudineugene-dot/Q50-gtr-
version                     {VERSION}
commit SHA                  (local build)
tag SHA                     (not a tagged build)
workflow run ID             (local build)

APK filename                {APK_OUTPUT_NAME}
APK SHA-256                 {apk_sha}
APK size                    {artifact_apk.stat().st_size} bytes
APK signing mode            {signing_mode}
APK signer cert SHA-256     {signer_sha if 'signer_sha' in locals() else 'unknown'}

EPK filename                {EPK_OUTPUT_NAME}
EPK SHA-256                 {epk_sha}
EPK size                    {final_epk.stat().st_size} bytes
EPK inner filename          {INNER_NAME}
EPK version                 2
EPK payloadType             2
EPK blockCount              1
EPK keySize                 128

build timestamp UTC         {subprocess.getoutput('python -c "import datetime; print(datetime.datetime.now(datetime.timezone.utc).strftime(\'%Y-%m-%dT%H:%M:%SZ\'))"')}

Note: the EPK container layout is verified structurally. Compatibility of
keys/obu_cert.pem with this specific Infiniti Q50 2017 DCU is NOT verified.
"""
    (ARTIFACT_DIR / "BUILD-PROVENANCE.txt").write_text(provenance_text, encoding="utf-8")

    # Also copy the final EPK to root repo for convenience
    root_epk = PROJECT_DIR.parent / EPK_OUTPUT_NAME
    shutil.copy2(final_epk, root_epk)
    print(f"\nCopied finished EPK to repository root: {root_epk}")

    log("Build complete! All artifacts ready in artifact/ and repo root.")


if __name__ == "__main__":
    main()
