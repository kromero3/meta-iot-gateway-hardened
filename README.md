# meta-iot-gateway-hardened

**TP de Specialisation — Securite Informatique**

Layer Yocto (branche **scarthgap**) qui construit une image Linux durcie pour
une passerelle IoT. L'image `iot-gateway-image` verrouille l'acces distant,
rend le systeme de fichiers immuable, active les flags de durcissement du
toolchain et deploie un pare-feu iptables strict. Une analyse CVE
(`cve-check`) est activee au build.

## Auteurs

- **ROMERO Kenael**
- **JOUHANNET Louis**
- **AVRIL Samuel**
- **RIBEIRO Enzo**

## Sommaire

- [Livrables](#livrables)
- [Structure du layer](#structure-du-layer)
- [Mesures de durcissement](#mesures-de-durcissement)
- [Prerequis](#prerequis)
- [Integration du layer](#integration-du-layer-etape-par-etape)
- [Compilation](#compilation)
- [Demarrage et tests de validation](#demarrage-et-tests-de-validation)
- [Analyse CVE](#analyse-cve)
- [Rapports et captures d-ecran](#rapports-et-captures-decran)

## Livrables

| Livrable | Emplacement |
|---|---|
| Layer complet | `meta-iot-gateway-hardened/` |
| Rapport d'audit securite | [`docs/Rapport_Audit_Securite_IoT_Gateway.pdf`](docs/Rapport_Audit_Securite_IoT_Gateway.pdf) |
| Compte rendu TP0 (prise en main Yocto) | [`docs/Compte rendu.pdf`](docs/Compte%20rendu.pdf) |
| Captures d'ecran de validation | `docs/screenshots/` |

## Structure du layer

```text
meta-iot-gateway-hardened/
├── conf/
│   └── layer.conf                        # declaration du layer (compat scarthgap)
├── recipes-connectivity/
│   └── openssh/
│       ├── openssh_%.bbappend            # ajoute notre sshd_config
│       └── files/
│           └── sshd_config               # sshd_config durci (PermitRootLogin no...)
└── recipes-core/
    ├── firewall-config/
    │   ├── firewall-config_1.0.bb        # recette du script iptables
    │   └── files/
    │       └── firewall-config.sh        # script init.d charge au boot
    └── images/
        └── iot-gateway-image.bb          # recette d'image principale
```

## Mesures de durcissement

| # | Mesure | Mecanisme | Preuve |
|---|---|---|---|
| 1 | Rootfs en lecture seule | `IMAGE_FEATURES += "read-only-rootfs"` | [`03-touch-readonly`](docs/screenshots/03-touch-readonly.png) |
| 2 | Serveur SSH minimal | `IMAGE_FEATURES += "ssh-server-openssh"` | [`02-login-admin-ok`](docs/screenshots/02-login-admin-ok.png) |
| 3 | Interdiction root SSH | `sshd_config`: `PermitRootLogin no` | [`01-ssh-root-refuse`](docs/screenshots/01-ssh-root-refuse.png) |
| 4 | Utilisateur `admin` (groupe wheel) | `EXTRA_USERS_PARAMS` + `inherit extrausers` | [`02-login-admin-ok`](docs/screenshots/02-login-admin-ok.png) |
| 5 | Compte `root` verrouille | `usermod -L root` | [`01-ssh-root-refuse`](docs/screenshots/01-ssh-root-refuse.png) |
| 6 | Pare-feu iptables strict | Recette `firewall-config` + `update-rc.d` | [`04-iptables-L-n`](docs/screenshots/04-iptables-L-n.png) |
| 7 | Flags de durcissement toolchain | `require conf/distro/include/security_flags.inc` | inclus par defaut dans Poky (`meta-poky/conf/distro/poky.conf`) |
| 8 | Analyse CVE au build | `INHERIT += "cve-check"` | [`05-cve-check-config`](docs/screenshots/05-cve-check-config.png) |

Details techniques et justifications : voir
[`docs/Rapport_Audit_Securite_IoT_Gateway.pdf`](docs/Rapport_Audit_Securite_IoT_Gateway.pdf).

> **Note sur la mesure 6** : la sortie observee de `iptables -L -n` montre
> `INPUT policy DROP` avec `lo` autorise, mais les regles specifiques
> (`--dport 22`, `-m conntrack`) n'apparaissent pas car le kernel Yocto
> `qemux86-64` par defaut ne compile pas les modules `xt_tcp` /
> `xt_conntrack`. Le mecanisme deny-by-default reste demontre. Un BSP de
> production ajouterait ces modules via un fragment de config kernel.

## Prerequis

- Machine hote **Ubuntu 22.04 LTS** recommandee (l'equipe a valide sous
  Ubuntu 26.04 LTS avec deux correctifs mineurs, voir Compte rendu §2).
- Environ **80 Go** d'espace disque libre pour un build complet.
- Yocto **scarthgap** (Poky 5.0.19, BitBake 2.8.1).
- Machine cible : `qemux86-64` (par defaut).

Dependances hote (Ubuntu) :

```bash
sudo apt update
sudo apt install -y gawk wget git diffstat unzip texinfo gcc build-essential \
    chrpath socat cpio python3 python3-pip python3-pexpect xz-utils \
    debianutils iputils-ping python3-git python3-jinja2 libegl1-mesa \
    libsdl1.2-dev pylint xterm python3-subunit mesa-common-dev zstd liblz4-tool
```

> **Ubuntu 26.04** : remplacer `libegl1-mesa` par `libegl1` et `liblz4-tool`
> par `lz4`. Voir Compte rendu §2 pour la procedure complete (locale UTF-8,
> AppArmor userns).

## Integration du layer (etape par etape)

### 1. Recuperer Poky et les layers de dependances

```bash
cd ~
git clone -b scarthgap https://git.yoctoproject.org/poky
cd poky

# Layers utilises par meta-security et par ce layer
git clone -b scarthgap https://git.openembedded.org/meta-openembedded
git clone -b scarthgap https://git.yoctoproject.org/meta-security
```

### 2. Cloner ce depot dans l'arbre Poky

```bash
cd ~/poky
git clone https://github.com/<votre-user>/meta-iot-gateway-hardened.git
```

> Placez le dossier `meta-iot-gateway-hardened/` du repo directement au meme
> niveau que `meta-poky`, `meta-openembedded`, `meta-security`.

### 3. Initialiser l'environnement de build

```bash
cd ~/poky
source oe-init-build-env build
```

Cette commande cree `~/poky/build/` et positionne le shell dedans.

### 4. Activer les layers dans `conf/bblayers.conf`

Ajouter (chemin `bitbake-layers` recommande, ordre important a cause des
dependances) :

```bash
bitbake-layers add-layer ../meta-openembedded/meta-oe
bitbake-layers add-layer ../meta-openembedded/meta-python
bitbake-layers add-layer ../meta-openembedded/meta-networking
bitbake-layers add-layer ../meta-security
bitbake-layers add-layer ../meta-iot-gateway-hardened
```

Verification :

```bash
bitbake-layers show-layers
```

### 5. Ajouter les hardening globaux a `conf/local.conf`

```conf
# --- Machine cible ---
MACHINE ??= "qemux86-64"

# --- Durcissement du toolchain (stack-protector, FORTIFY, RELRO, NX) ---
require conf/distro/include/security_flags.inc

# --- Analyse CVE au build ---
INHERIT += "cve-check"
# Ne conserver dans le rapport que les CVE non corrigees :
CVE_CHECK_REPORT_PATCHED = "0"

# --- (Bonus pedagogique) Force une version vulnerable pour generer des
# CVE "High" dans le rapport. A commenter pour un vrai build.
# PREFERRED_VERSION_curl = "7.50.0"
```

> **Attention** : les flags de `security_flags.inc` peuvent faire echouer
> certains paquets exotiques. On les desactive au cas par cas via
> `SECURITY_CFLAGS:pn-<recette> = ""`.

## Compilation

```bash
cd ~/poky
source oe-init-build-env build
bitbake iot-gateway-image
```

Pour reprendre malgre une erreur non bloquante :

```bash
bitbake -k iot-gateway-image
```

## Demarrage et tests de validation

Lancer l'image dans QEMU (sans fenetre graphique, adapte a un serveur SSH) :

```bash
runqemu qemux86-64 nographic slirp
```

Depuis le shell QEMU :

### Test 1 - Connexion SSH root (doit **echouer**)

```bash
ssh root@192.168.7.2
# Attendu : Permission denied
```

### Test 2 - Connexion SSH admin (doit **reussir**)

```bash
ssh admin@192.168.7.2
# Le mot de passe correspondant au hash defini dans
# meta-iot-gateway-hardened/recipes-core/images/iot-gateway-image.bb
# (variable EXTRA_USERS_PARAMS). Voir rapport d'audit pour les credentials
# du build de reference.
```

### Test 3 - Ecriture a la racine (doit **echouer**, rootfs read-only)

```bash
touch /test
# Attendu : touch: cannot touch '/test': Read-only file system
```

### Test 4 - Regles du pare-feu

```bash
iptables -L -n
# Chain INPUT (policy DROP)
# ACCEPT     all  --  0.0.0.0/0   0.0.0.0/0
# ACCEPT     all  --  0.0.0.0/0   0.0.0.0/0   state ESTABLISHED,RELATED
# ACCEPT     tcp  --  0.0.0.0/0   0.0.0.0/0   tcp dpt:22
```

### Test 5 - Rapport CVE

```bash
cat tmp/deploy/cve/iot-gateway-image
# ou (JSON)
cat tmp/deploy/cve/iot-gateway-image.json | jq .
```

## Analyse CVE

Les rapports sont generes automatiquement dans :

- `tmp/deploy/cve/iot-gateway-image` (texte)
- `tmp/deploy/cve/iot-gateway-image.json` (JSON)

Une analyse detaillee d'une CVE "High" trouvee dans le build et sa strategie
de remediation (mise a jour de la recette / backport de patch) est fournie
dans [`docs/Rapport_Audit_Securite_IoT_Gateway.pdf`](docs/Rapport_Audit_Securite_IoT_Gateway.pdf) §6.

## Rapports et captures d'ecran

- **[Rapport d'audit de securite](docs/Rapport_Audit_Securite_IoT_Gateway.pdf)** :
  detaille chaque mesure, l'analyse CVE et la strategie de remediation.
- **[Compte rendu de prise en main Yocto (TP0)](docs/Compte%20rendu.pdf)** :
  installation de Poky, premier build, layers, sstate-cache.
- **Captures d'ecran de validation** : `docs/screenshots/`
  (ssh root refuse, ssh admin ok, touch / refuse, iptables -L, rapport CVE).

## Notes de portabilite

- Le hash de mot de passe present dans `iot-gateway-image.bb` (variable
  `EXTRA_USERS_PARAMS`) est un hash MD5 genere avec `openssl passwd -1`
  pour les besoins du TP. En production :
  - regenerer un nouveau hash avec un mot de passe fort :
    `openssl passwd -6 "<mot_de_passe_fort>"` (`-6` = SHA-512),
  - **desactiver l'authentification par mot de passe** au profit des cles
    SSH : `PubkeyAuthentication yes` + `PasswordAuthentication no` dans
    `sshd_config`,
  - ne jamais committer le hash de production dans un depot public.
- Le layer cible `qemux86-64` par defaut. Pour une carte reelle, changer
  `MACHINE` dans `local.conf` et ajouter le BSP correspondant a
  `bblayers.conf`.

## Licence

MIT — voir en-tetes des recettes.
