# Captures d'ecran de validation

Chaque test dispose de deux formats :
- `.txt` : sortie brute recuperee via `tmux capture-pane` depuis la session
  QEMU (`runqemu qemux86-64 iot-gateway-image nographic slirp`).
- `.png` : rendu terminal stylise pour lecture rapide.

| # | Fichier | Ce qui est prouve |
|---|---------|-------------------|
| 1 | `01-ssh-root-refuse.png` / `.txt` | Le compte `root` refuse le login (`Login incorrect`) |
| 2 | `02-login-admin-ok.png` / `.txt`  | L'utilisateur `admin` se connecte avec succes |
| 3 | `03-touch-readonly.png` / `.txt`  | `touch /test` echoue avec `Read-only file system` |
| 4 | `04-iptables-L-n.png` / `.txt`    | `INPUT policy DROP` + seul `lo` autorise (durcissement) |
| 5 | `05-cve-check-config.png` / `.txt` | Configuration `cve-check` active dans le build |

Tests realises sur l'image `iot-gateway-image` construite sur la branche
`scarthgap`, machine cible `qemux86-64`. Voir le rapport d'audit
(`../Rapport_Audit_Securite_IoT_Gateway.pdf`) pour l'analyse detaillee.
