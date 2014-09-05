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

(ns aipal.infra.eraajo.kayttajat
  (:require [clojurewerkz.quartzite.conversion :as qc]
            [clojure.tools.logging :as log]
            [oph.korma.korma-auth :refer [*current-user-uid*
                                          *current-user-oid*
                                          integraatiokayttaja]]
            [aipal.arkisto.kayttajaoikeus :as kayttajaoikeus-arkisto]
            [aipal.arkisto.koulutustoimija :as koulutustoimija-arkisto]
            [aipal.integraatio.kayttooikeuspalvelu :as kop]
            [aipal.toimiala.kayttajaroolit :refer [kayttajaroolit]]))

(defn paivita-kayttajat-ldapista [kayttooikeuspalvelu]
  (binding [*current-user-uid* integraatiokayttaja
            ;; Tietokantayhteyden avaus asettaa *current-user-oid*-promisen
            ;; arvon. Kun käsitellään HTTP-pyyntöä, auth-wrapper luo tämän
            ;; promisen. Koska tätä funktiota ei kutsuta HTTP-pyynnön
            ;; käsittelijästä, meidän täytyy luoda promise itse.
            *current-user-oid* (promise)]
    (let [oid->ytunnus (group-by :oid (koulutustoimija-arkisto/hae-kaikki-joissa-oid))]
      (log/info "Päivitetään käyttäjät ja käyttäjien roolit käyttöoikeuspalvelun LDAP:sta")
      (kayttajaoikeus-arkisto/paivita-kaikki!
        (apply concat (for [[_ rooli] kayttajaroolit]
                        (kop/kayttajat kayttooikeuspalvelu rooli oid->ytunnus)))))))

;; Cloverage ei tykkää `defrecord`eja generoivista makroista, joten hoidetaan
;; `defjob`:n homma käsin.
(defrecord PaivitaKayttajatLdapistaJob []
   org.quartz.Job
   (execute [this ctx]
     (let [{kop "kayttooikeuspalvelu"} (qc/from-job-data ctx)]
       (paivita-kayttajat-ldapista kop))))
