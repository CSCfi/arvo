#!/bin/bash
set -eu

yum -y install httpd

cp httpd/aipal.conf /etc/httpd/conf.d/
cp httpd/aipalvastaus.conf /etc/httpd/conf.d/

service httpd restart

# SELinux estää oletuksena mod_proxy:n käytön
(selinuxenabled && setsebool -P httpd_can_network_connect=1) || true

