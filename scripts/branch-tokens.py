#!/usr/bin/env python3
"""Was hat die Entwicklung dieses Branches an Claude-Code-Tokens gekostet?

Liest die Sitzungsprotokolle, die Claude Code unter ~/.claude/projects/<projekt>/
ablegt, und summiert den Verbrauch je Branch. Gedacht für die Beschreibung eines
Pull Requests:

    python3 scripts/branch-tokens.py                    # aktueller Branch, Markdown
    python3 scripts/branch-tokens.py --branch main      # ein bestimmter Branch
    python3 scripts/branch-tokens.py --all              # Rangliste aller Branches
    python3 scripts/branch-tokens.py --json             # maschinenlesbar

Und beim Anlegen eines PRs:

    gh pr create --body-file <(cat beschreibung.md; python3 scripts/branch-tokens.py)

Zwei Dinge, die man über die Zahlen wissen muss:

1. Arbeit, die vor dem Branch passiert — Spec-Gespräche, Planung, Recherche —
   liegt auf `main` und fehlt dem Feature. Das ist kein Randfall: über dieses
   Projekt hinweg entfällt gut ein Drittel des Verbrauchs auf `main`.
2. Cache-Reads sind keine frische Eingabe. Sie machen über 99 % der Eingabe aus
   und kosten einen Bruchteil. Eine addierte "Gesamt-Token"-Zahl wäre deshalb
   Angeberei; die vier Werte stehen getrennt.

Die Protokolle werden nach 30 Tagen gelöscht (`cleanupPeriodDays`) — für ältere
Branches liefert das Skript dann nichts mehr.
"""

from __future__ import annotations

import argparse
import json
import subprocess
import sys
from dataclasses import dataclass, field
from pathlib import Path

# Nur Zeilen, die beide Merkmale tragen, werden überhaupt geparst — das spart bei
# gut hundert Megabyte Protokoll den Großteil der Arbeit.
REQUIRED_MARKERS = (b'"usage"', b'"gitBranch"')


@dataclass
class Usage:
    """Verbrauch eines Branches. Requests zählt echte API-Antworten, nicht Zeilen."""

    output: int = 0
    fresh_input: int = 0
    cache_write: int = 0
    cache_read: int = 0
    requests: int = 0
    models: set[str] = field(default_factory=set)
    first: str | None = None
    last: str | None = None

    def add(self, usage: dict, model: str | None, timestamp: str | None) -> None:
        self.output += usage.get("output_tokens", 0) or 0
        self.fresh_input += usage.get("input_tokens", 0) or 0
        self.cache_write += usage.get("cache_creation_input_tokens", 0) or 0
        self.cache_read += usage.get("cache_read_input_tokens", 0) or 0
        self.requests += 1
        # "<synthetic>" markiert Meldungen, die Claude Code selbst erzeugt (etwa
        # Fehlertexte) — kein Modell und nichts, was Tokens gekostet hat.
        if model and not model.startswith("<"):
            self.models.add(model)
        if timestamp:
            if self.first is None or timestamp < self.first:
                self.first = timestamp
            if self.last is None or timestamp > self.last:
                self.last = timestamp


def project_directory(repo_root: Path) -> Path:
    """Claude Code legt je Arbeitsverzeichnis einen Ordner an, Pfadtrenner werden zu '-'."""
    slug = str(repo_root).replace("/", "-")
    return Path.home() / ".claude" / "projects" / slug


def collect(project_dir: Path) -> dict[str, Usage]:
    """Verbrauch je Branch, dedupliziert.

    Claude Code schreibt eine Zeile je Inhaltsblock einer Antwort (Denken, Text,
    Werkzeugaufruf …), alle mit derselben message.id und derselben usage. Ohne
    Deduplizierung zählt man dieselbe Antwort mehrfach — im Bestand dieses Projekts
    rund 75 % zu viel.
    """
    per_branch: dict[str, Usage] = {}
    seen: set[str] = set()
    # Sitzungen und die Protokolle ihrer Subagenten; letztere tragen denselben Branch.
    for path in sorted(project_dir.rglob("*.jsonl")):
        with path.open("rb") as handle:
            for raw in handle:
                if not all(marker in raw for marker in REQUIRED_MARKERS):
                    continue
                try:
                    entry = json.loads(raw)
                except json.JSONDecodeError:
                    continue  # abgeschnittene letzte Zeile einer laufenden Sitzung
                message = entry.get("message") or {}
                usage = message.get("usage")
                branch = entry.get("gitBranch")
                if not usage or not branch:
                    continue
                message_id = message.get("id")
                if message_id:
                    if message_id in seen:
                        continue
                    seen.add(message_id)
                per_branch.setdefault(branch, Usage()).add(
                    usage, message.get("model"), entry.get("timestamp")
                )
    return per_branch


def current_branch(repo_root: Path) -> str:
    return subprocess.run(
        ["git", "-C", str(repo_root), "rev-parse", "--abbrev-ref", "HEAD"],
        capture_output=True, text=True, check=True,
    ).stdout.strip()


def repository_root() -> Path:
    return Path(subprocess.run(
        ["git", "rev-parse", "--show-toplevel"],
        capture_output=True, text=True, check=True,
    ).stdout.strip())


def thousands(value: int) -> str:
    return f"{value:,}".replace(",", ".")


def millions(value: int) -> str:
    if value < 1_000_000:
        return thousands(value)
    return f"{value / 1_000_000:.1f}".replace(".", ",") + " M"


def day(timestamp: str | None) -> str:
    return timestamp[:10] if timestamp else "?"


def markdown(branch: str, usage: Usage) -> str:
    models = ", ".join(sorted(usage.models)) or "unbekannt"
    period = day(usage.first) if day(usage.first) == day(usage.last) else f"{day(usage.first)} bis {day(usage.last)}"
    return "\n".join([
        "",
        "---",
        "",
        "<details>",
        f"<summary>Entwicklungsaufwand: {thousands(usage.output)} Ausgabe-Tokens "
        f"in {thousands(usage.requests)} Anfragen</summary>",
        "",
        f"Branch `{branch}`, {period}, Modelle: {models}.",
        "",
        "| | Tokens |",
        "|---|---|",
        f"| Ausgabe | {thousands(usage.output)} |",
        f"| Eingabe (frisch) | {thousands(usage.fresh_input)} |",
        f"| Cache-Write | {thousands(usage.cache_write)} |",
        f"| Cache-Read | {millions(usage.cache_read)} |",
        "",
        "Die vier Werte stehen bewusst getrennt: Cache-Reads machen über 99 % der",
        "Eingabe aus und kosten einen Bruchteil frischer Eingabe-Tokens.",
        "",
        "Nicht enthalten ist, was vor dem Branch anfiel — Spec-Gespräche, Planung und",
        "Recherche liegen auf `main`. Die Zahl taugt zum Vergleich zwischen Features,",
        "nicht als vollständige Kostenzuordnung.",
        "</details>",
    ])


def ranking(per_branch: dict[str, Usage]) -> str:
    rows = sorted(per_branch.items(), key=lambda item: item[1].output, reverse=True)
    width = max((len(name) for name, _ in rows), default=6)
    lines = [f"{'Branch'.ljust(width)}  {'Ausgabe'.rjust(11)}  {'Cache-Read'.rjust(10)}  Anfragen"]
    for name, usage in rows:
        lines.append(
            f"{name.ljust(width)}  {thousands(usage.output).rjust(11)}  "
            f"{millions(usage.cache_read).rjust(10)}  {usage.requests}"
        )
    return "\n".join(lines)


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--branch", help="Branch statt des aktuellen")
    parser.add_argument("--all", action="store_true", help="Rangliste über alle Branches")
    parser.add_argument("--json", action="store_true", help="maschinenlesbare Ausgabe")
    args = parser.parse_args()

    root = repository_root()
    project_dir = project_directory(root)
    if not project_dir.is_dir():
        print(f"Keine Sitzungsprotokolle unter {project_dir}", file=sys.stderr)
        return 1

    per_branch = collect(project_dir)
    if args.all:
        print(ranking(per_branch))
        return 0

    branch = args.branch or current_branch(root)
    usage = per_branch.get(branch)
    if usage is None:
        print(f"Kein Verbrauch für Branch '{branch}' gefunden "
              f"(Protokolle werden nach 30 Tagen gelöscht).", file=sys.stderr)
        return 1

    if args.json:
        print(json.dumps({
            "branch": branch,
            "output": usage.output,
            "freshInput": usage.fresh_input,
            "cacheWrite": usage.cache_write,
            "cacheRead": usage.cache_read,
            "requests": usage.requests,
            "models": sorted(usage.models),
            "first": usage.first,
            "last": usage.last,
        }, indent=2, ensure_ascii=False))
    else:
        print(markdown(branch, usage))
    return 0


if __name__ == "__main__":
    sys.exit(main())
