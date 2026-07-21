#!/bin/sh
# firewall-config.sh
# Charge un jeu de regles iptables strict au demarrage du systeme.
# Installe via inherit update-rc.d, priorite 99, runlevel S.

set -e

# --- Purge de la configuration existante ---------------------------------
iptables -F
iptables -X
iptables -Z

# --- Politiques par defaut : deny-all -------------------------------------
iptables -P INPUT DROP
iptables -P FORWARD DROP
iptables -P OUTPUT ACCEPT

# --- Trafic autorise ------------------------------------------------------
# Loopback
iptables -A INPUT -i lo -j ACCEPT

# Connexions deja etablies (utilise -m conntrack, plus recent que -m state)
iptables -A INPUT -m conntrack --ctstate ESTABLISHED,RELATED -j ACCEPT

# SSH entrant (port 22)
iptables -A INPUT -p tcp --dport 22 -j ACCEPT

# ICMP (ping) : autorise pour le diagnostic
iptables -A INPUT -p icmp --icmp-type echo-request -j ACCEPT

exit 0
