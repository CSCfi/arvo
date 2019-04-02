;; Copyright (c) 2013 The Finnish National Board of Education - Opetushallitus
;;
;; This program is free software:  Licensed under the EUPL, Version 1.1 or - as
;; soon as they will be approved by the European Commission - subsequent versions
;; of the EUPL (the "Licence");
;;
;; You may not use this work except in compliance with the Licence.
;; You may obtain a copy of the Licence at: http://www.osor.eu/eupl/
;;
;; This program is distributed in the hope that it will be useful,
;; but WITHOUT ANY WARRANTY; without even the implied warranty of
;; MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
;; European Union Public Licence for more details.

(ns aipal.toimiala.kayttajaroolit
  "https://confluence.csc.fi/pages/viewpage.action?pageId=53514648")

(def ldap-ryhma->rooli {"YLLAPITAJA" "YLLAPITAJA"
                        "KATSELIJA" "KATSELIJA"
                        "AMKVASTUUKAYTTAJA" "OPL-VASTUUKAYTTAJA"
                        "AMKKAYTTAJA" "OPL-KAYTTAJA"
                        "AMKKATSELIJA" "OPL-KATSELIJA"
                        "ARVO-YO-VASTUUKAYTTAJA" "OPL-VASTUUKAYTTAJA"
                        "ARVO-YO-KAYTTAJA" "OPL-KAYTTAJA"
                        "ARVO-YO-KATSELIJA" "OPL-KATSELIJA"
                        "ARVO-KT-VASTUUKAYTTAJA" "OPL-VASTUUKAYTTAJA"
                        "ARVO-KT-KYSELYKERTAKAYTTAJA" "OPL-KYSELYKERTAKAYTTAJA"
                        "ARVO-KT-KAYTTAJA" "OPL-KAYTTAJA"
                        "ARVO-KT-KATSELIJA" "OPL-KATSELIJA"})


;; roolit jotka liittyvät koulutustoimijaan
(def koulutustoimija-roolit #{"OPL-VASTUUKAYTTAJA" "OPL-KYSELYKERTAKAYTTAJA" "OPL-KAYTTAJA" "OPL-KATSELIJA"})

(def oph-roolit #{"YLLAPITAJA"})

(def roolijarjestys
  "Roolien prioriteettijärjestys, jolla valitaan aktiivinen rooli jos käyttäjä ei ole sitä valinnut"
  {"YLLAPITAJA" 0
   "OPL-VASTUUKAYTTAJA" 2
   "OPL-KYSELYKERTAKAYTTAJA" 3
   "OPL-KAYTTAJA" 4
   "OPL-KATSELIJA" 5
   "KATSELIJA" 6})
