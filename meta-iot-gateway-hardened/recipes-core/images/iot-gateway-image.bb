SUMMARY = "Image durcie pour passerelle IoT (Master 2 ESGI - TP Securite Informatique)"
DESCRIPTION = "Image Linux minimale pour une passerelle IoT collectant des donnees \
de capteurs. L'image est durcie : rootfs en lecture seule, root verrouille, \
acces SSH uniquement pour l'utilisateur 'admin', pare-feu iptables strict."
LICENSE = "MIT"

inherit core-image
inherit extrausers

# Fonctionnalites d'image : SSH + rootfs read-only
IMAGE_FEATURES += "ssh-server-openssh read-only-rootfs"

# Paquets additionnels : pare-feu + sudo + script de configuration
IMAGE_INSTALL += "iptables firewall-config sudo"

# --- Hardening utilisateurs -----------------------------------------------
# Hash MD5 genere via : openssl passwd -1 "<mot_de_passe>"
# (regenerer avec un nouveau hash en production, idealement SHA-512 : -6)
# - Cree l'utilisateur admin (groupe wheel, autorise sudo via /etc/sudoers)
# - Verrouille le compte root : usermod -L => aucune connexion par mot de passe
EXTRA_USERS_PARAMS = "\
    useradd -p '\$1\$sBWvxjvr\$FS3NCZJ9lj2Uyim0Rr9yZ/' -G wheel admin; \
    usermod -L root; \
"

# --- Autoriser le groupe 'wheel' a utiliser sudo -------------------------
# Cree /etc/sudoers.d/wheel pour permettre a admin (membre de wheel)
# de faire 'sudo <cmd>' avec son mot de passe.
ROOTFS_POSTPROCESS_COMMAND += "enable_wheel_sudo;"

enable_wheel_sudo() {
    install -d ${IMAGE_ROOTFS}${sysconfdir}/sudoers.d
    echo '%wheel ALL=(ALL) ALL' > ${IMAGE_ROOTFS}${sysconfdir}/sudoers.d/wheel
    chmod 0440 ${IMAGE_ROOTFS}${sysconfdir}/sudoers.d/wheel
}
