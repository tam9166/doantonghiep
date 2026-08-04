from __future__ import annotations

from pathlib import Path


ROOTS = (
    Path(r"E:\DoAnTotNghiep\quanlynhahang\src"),
    Path(r"E:\DoAnTotNghiep\Frontend\nha-hang-frontend\src"),
)
SUFFIXES = {".java", ".vue", ".js", ".ts", ".css", ".sql", ".properties", ".json"}
SKIP_PARTS = {"static", "target", "node_modules"}
MARKERS = ("Ãƒ", "Ã‚", "Ã„", "Ã†", "Ã¡Âº", "Ã¡Â»", "Ã¢â‚¬", "Ã°Å¸")
CP1252 = {
    "€": 0x80, "‚": 0x82, "ƒ": 0x83, "„": 0x84, "…": 0x85, "†": 0x86,
    "‡": 0x87, "ˆ": 0x88, "‰": 0x89, "Š": 0x8A, "‹": 0x8B, "Œ": 0x8C,
    "Ž": 0x8E, "‘": 0x91, "’": 0x92, "“": 0x93, "”": 0x94, "•": 0x95,
    "–": 0x96, "—": 0x97, "˜": 0x98, "™": 0x99, "š": 0x9A, "›": 0x9B,
    "œ": 0x9C, "ž": 0x9E, "Ÿ": 0x9F,
}


def score(value: str) -> int:
    total = sum(value.count(marker) for marker in MARKERS)
    for index, char in enumerate(value):
        if char == "Ă" and index + 1 < len(value) and ord(value[index + 1]) >= 0xA0:
            total += 2
        if 0x80 <= ord(char) <= 0x9F:
            total += 4
    return total


def cp1252_bytes(value: str) -> bytes | None:
    output = bytearray()
    for char in value:
        if ord(char) <= 0xFF:
            output.append(ord(char))
        elif char in CP1252:
            output.append(CP1252[char])
        else:
            return None
    return bytes(output)


def candidates(value: str) -> list[str]:
    result: list[str] = []
    raw = cp1252_bytes(value)
    if raw is not None:
        try:
            result.append(raw.decode("utf-8"))
        except UnicodeDecodeError:
            pass
    try:
        raw = value.encode("windows-1258")
        if raw.decode("windows-1258") == value:
            result.append(raw.decode("utf-8"))
    except UnicodeError:
        pass
    return result


def repair(value: str) -> str:
    current = value
    current_score = score(current)
    for _ in range(3):
        options = [(score(option), option) for option in candidates(current)]
        options = [(option_score, option) for option_score, option in options if option_score < current_score]
        if not options:
            break
        current_score, current = min(options, key=lambda item: item[0])
    return current


def main() -> None:
    changed: list[Path] = []
    for root in ROOTS:
        for path in root.rglob("*"):
            if path.suffix not in SUFFIXES or any(part in SKIP_PARTS for part in path.parts):
                continue
            source = path.read_text(encoding="utf-8")
            fixed = repair(source)
            if fixed != source:
                path.write_text(fixed, encoding="utf-8", newline="")
                changed.append(path)
    print(f"Repaired {len(changed)} source files")
    for path in changed:
        print(path)


if __name__ == "__main__":
    main()
