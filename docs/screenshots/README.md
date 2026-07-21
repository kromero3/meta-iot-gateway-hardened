# Captures d'ecran de validation

Placer ici les captures prouvant l'efficacite des mesures de durcissement :

- `01-ssh-root-refuse.png` : `ssh root@192.168.7.2` refuse (Permission denied)
- `02-ssh-admin-ok.png` : connexion SSH `admin@192.168.7.2` reussie
- `03-touch-readonly.png` : `touch /test` echoue avec "Read-only file system"
- `04-iptables-L.png` : sortie de `iptables -L -n` (policy INPUT DROP + SSH allow)
- `05-cve-report.png` : extrait du rapport `tmp/deploy/cve/iot-gateway-image`

Les captures existantes sont referencees dans le rapport d'audit (`../Rapport_Audit_Securite_IoT_Gateway.pdf`).
