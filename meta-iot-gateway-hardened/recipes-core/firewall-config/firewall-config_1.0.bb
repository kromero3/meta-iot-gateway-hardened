SUMMARY = "Firewall configuration script for IoT gateway"
DESCRIPTION = "Script d'initialisation iptables charge au demarrage via update-rc.d. \
Politique par defaut : DROP en INPUT/FORWARD, ACCEPT en OUTPUT, autorise \
uniquement lo, les connexions etablies et SSH (port 22)."
LICENSE = "MIT"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/MIT;md5=0835ade698e0bcf8506ecda2f7b4f302"

SRC_URI = "file://firewall-config.sh"

S = "${WORKDIR}"

inherit update-rc.d

INITSCRIPT_NAME = "firewall-config.sh"
INITSCRIPT_PARAMS = "start 99 S ."

RDEPENDS:${PN} = "iptables"

do_install() {
    install -d ${D}${sysconfdir}/init.d
    install -m 0755 ${WORKDIR}/firewall-config.sh ${D}${sysconfdir}/init.d/firewall-config.sh
}

FILES:${PN} = "${sysconfdir}/init.d/firewall-config.sh"
