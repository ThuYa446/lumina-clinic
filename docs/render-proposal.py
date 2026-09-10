"""Render the short, repository-owned proposal Markdown to printable HTML/PDF.

Only Python's standard library is needed. PDF output additionally needs local
Chrome or Edge: python docs/render-proposal.py --browser "path/to/chrome.exe"
This small renderer supports just the Markdown constructs used in proposal.md.
"""
from argparse import ArgumentParser
from html import escape
from pathlib import Path
import re
import subprocess
import tempfile


def inline(value: str) -> str:
    value = escape(value)
    value = re.sub(r"\*\*(.+?)\*\*", r"<strong>\1</strong>", value)
    return re.sub(r"\[([^]]+)\]\(([^)]+)\)", r'<a href="\2">\1</a>', value)


def render(source: str) -> str:
    result = []
    lines = source.splitlines()
    i = 0
    while i < len(lines):
        line = lines[i]
        if not line.strip():
            i += 1
            continue
        if line.startswith("#"):
            level = len(line) - len(line.lstrip("#"))
            result.append(f"<h{level}>{inline(line[level:].strip())}</h{level}>")
        elif line.startswith("|"):
            rows = []
            while i < len(lines) and lines[i].startswith("|"):
                cells = [cell.strip() for cell in lines[i].strip("|").split("|")]
                if not all(re.fullmatch(r"[-: ]+", cell) for cell in cells):
                    tag = "th" if not rows else "td"
                    rows.append("<tr>" + "".join(f"<{tag}>{inline(cell)}</{tag}>" for cell in cells) + "</tr>")
                i += 1
            result.append("<table>" + "".join(rows) + "</table>")
            continue
        elif re.match(r"\d+\. ", line):
            items = []
            while i < len(lines) and re.match(r"\d+\. ", lines[i]):
                items.append("<li>" + inline(re.sub(r"^\d+\. ", "", lines[i])) + "</li>")
                i += 1
            result.append("<ol>" + "".join(items) + "</ol>")
            continue
        else:
            result.append("<p>" + inline(line) + "</p>")
        i += 1
    return "\n".join(result)


def main() -> None:
    parser = ArgumentParser()
    parser.add_argument("--browser", help="Local Chrome or Edge executable; omit to create only HTML")
    args = parser.parse_args()
    directory = Path(__file__).resolve().parent
    body = render((directory / "proposal.md").read_text(encoding="utf-8"))
    html = """<!doctype html><html lang="en"><head><meta charset="utf-8">
<title>Lumina Clinics — Founder proposal</title><style>
@page { size: A4; margin: 17mm 18mm; }
body { max-width: 760px; margin: 35px auto; color: #24332f; background: white;
       font: 10.5pt/1.42 Arial, sans-serif; }
h1 { font: 27pt/1.08 Georgia, serif; color: #164a3d; margin: 0 0 14px; }
h2 { font-size: 13pt; color: #164a3d; margin: 22px 0 8px; break-after: avoid; }
p { margin: 0 0 10px; orphans: 3; widows: 3; }
li { padding: 0 0 5px 3px; }
ol { padding-left: 22px; }
table { border-collapse: collapse; width: 100%; font-size: 10pt; }
th, td { text-align: left; padding: 7px 9px; border-bottom: 1px solid #dbe5df; vertical-align: top; }
th { background: #edf3ef; }
tr { break-inside: avoid; }
td:first-child { width: 42px; font-weight: bold; }
a { color: #164a3d; }
@media print { body { margin: 0; max-width: none; } }
</style></head><body>""" + body + "</body></html>"
    html_path = directory / "proposal.html"
    html_path.write_text(html, encoding="utf-8")
    print(f"HTML: {html_path}")
    if args.browser:
        pdf_path = directory / "proposal.pdf"
        with tempfile.TemporaryDirectory(prefix="lumina-proposal-") as browser_profile:
            command = [args.browser, "--headless", "--disable-gpu", "--no-first-run", "--no-default-browser-check",
                       "--no-pdf-header-footer", f"--user-data-dir={browser_profile}",
                       f"--print-to-pdf={pdf_path}", html_path.as_uri()]
            result = subprocess.run(command, capture_output=True, timeout=45)
            if result.returncode or not pdf_path.is_file():
                raise RuntimeError(result.stderr.decode(errors="replace"))
        print(f"PDF: {pdf_path}")


if __name__ == "__main__":
    main()
