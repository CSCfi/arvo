;; Copyright (c) 2014 The Finnish National Board of Education - Opetushallitus
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

(ns aipal.integraatio.organisaatiopalvelu
  (:require [aipal.arkisto.koulutustoimija :as koulutustoimija-arkisto]
            [aipal.arkisto.oppilaitos :as oppilaitos-arkisto]
            [aipal.arkisto.toimipaikka :as toimipaikka-arkisto]
            [clj-time.core :as time]
            [oph.common.util.util :refer [get-json-from-url map-by diff-maps some-value]]
            [clojure.tools.logging :as log]))

(defn halutut-kentat [koodi]
  (select-keys koodi [:nimi :oppilaitosTyyppiUri :postiosoite :yhteystiedot :virastoTunnus :ytunnus :oppilaitosKoodi :toimipistekoodi :oid :tyypit :parentOid]))

(defn hae-kaikki [url]
  (let [oids (get-json-from-url url)]
    (for [oid oids]
      (halutut-kentat (get-json-from-url (str url oid))))))


;; Koodistopalvelun oppilaitostyyppikoodistosta
(def ^:private halutut-tyypit
  #{"oppilaitostyyppi_21" ;; Ammatilliset oppilaitokset
    "oppilaitostyyppi_22" ;; Ammatilliset erityisoppilaitokset
    "oppilaitostyyppi_23" ;; Ammatilliset erikoisoppilaitokset
    "oppilaitostyyppi_24" ;; Ammatilliset aikuiskoulutuskeskukset
    "oppilaitostyyppi_41" ;; Ammattikorkeakoulut
    "oppilaitostyyppi_42" ;; Yliopistot
    "oppilaitostyyppi_61" ;; Musiikkioppilaitokset
    "oppilaitostyyppi_62" ;; Liikunnan koulutuskeskukset
    "oppilaitostyyppi_63" ;; Kansanopistot
    "oppilaitostyyppi_93" ;; Muut koulutuksen järjestäjät
    "oppilaitostyyppi_99" ;; Muut oppilaitokset
    "oppilaitostyyppi_xx" ;; Tyyppi ei tiedossa
    })

(defn ^:private haluttu-tyyppi? [koodi]
  (when-let [tyyppi (:oppilaitosTyyppiUri koodi)]
    (contains? halutut-tyypit (subs tyyppi 0 19))))

(defn ^:private nimi [koodi]
  ((some-fn :fi :sv :en) (:nimi koodi)))

(defn ^:private nimi-sv [koodi]
  ((some-fn :sv :fi :en) (:nimi koodi)))

(defn ^:private postinumero [koodi]
  (when-let [postinumerokoodi (get-in koodi [:postiosoite :postinumeroUri])]
    (subs postinumerokoodi 6)))

(defn ^:private email [koodi]
  (some :email (:yhteystiedot koodi)))

(defn ^:private www-osoite [koodi]
  (some :www (:yhteystiedot koodi)))

(defn ^:private puhelin [koodi]
  (:numero (some-value #(= "puhelin" (:tyyppi %)) (:yhteystiedot koodi))))

(defn ^:private y-tunnus [koodi]
  (or (:ytunnus koodi) (:virastoTunnus koodi)))

(defn ^:private koodi->koulutustoimija [koodi]
  {:nimi_fi (nimi koodi)
   :nimi_sv (nimi-sv koodi)
   :oid (:oid koodi)
   :sahkoposti (email koodi)
   :puhelin (puhelin koodi)
   :osoite (get-in koodi [:postiosoite :osoite])
   :postinumero (postinumero koodi)
   :postitoimipaikka (get-in koodi [:postiosoite :postitoimipaikka])
   :www_osoite (www-osoite koodi)
   :ytunnus (y-tunnus koodi)})

(defn ^:private koodi->oppilaitos [koodi]
  {:nimi_fi (nimi koodi)
   :nimi_sv (nimi-sv koodi)
   :oid (:oid koodi)
   :sahkoposti (email koodi)
   :puhelin (puhelin koodi)
   :osoite (get-in koodi [:postiosoite :osoite])
   :postinumero (postinumero koodi)
   :postitoimipaikka (get-in koodi [:postiosoite :postitoimipaikka])
   :www_osoite (www-osoite koodi)
   :oppilaitoskoodi (:oppilaitosKoodi koodi)})

(defn ^:private koodi->toimipaikka [koodi]
  {:nimi_fi (nimi koodi)
   :nimi_sv (nimi-sv koodi)
   :oid (:oid koodi)
   :sahkoposti (email koodi)
   :puhelin (puhelin koodi)
   :osoite (get-in koodi [:postiosoite :osoite])
   :postinumero (postinumero koodi)
   :postitoimipaikka (get-in koodi [:postiosoite :postitoimipaikka])
   :www_osoite (www-osoite koodi)
   :toimipaikkakoodi (:toimipistekoodi koodi)})

(def ^:private yhteiset-kentat [:nimi_fi :nimi_sv :oid :sahkoposti :puhelin :osoite
                                :postinumero :postitoimipaikka :www_osoite])

(defn ^:private koulutustoimijan-kentat [koulutustoimija]
  (when koulutustoimija
    (select-keys koulutustoimija (conj yhteiset-kentat :ytunnus))))

(defn ^:private oppilaitoksen-kentat [oppilaitos]
  (when oppilaitos
    (select-keys oppilaitos (conj yhteiset-kentat :oppilaitoskoodi :koulutustoimija))))

(defn ^:private toimipaikan-kentat [toimipaikka]
  (when toimipaikka
    (select-keys toimipaikka (conj yhteiset-kentat :toimipaikkakoodi :oppilaitos))))

(defn ^:private tyyppi [koodi]
  (cond
    (some #{"Koulutustoimija"} (:tyypit koodi)) :koulutustoimija
    (haluttu-tyyppi? koodi) :oppilaitos
    (:toimipistekoodi koodi) :toimipaikka))

(defn generoi-oid->y-tunnus [koulutustoimijakoodit oppilaitoskoodit]
  (loop [oid->ytunnus (into {} (for [kt koulutustoimijakoodit]
                                 [(:oid kt) (y-tunnus kt)]))
         oppilaitoskoodit oppilaitoskoodit]
    (let [uudet (for [o oppilaitoskoodit
                      :when (contains? oid->ytunnus (:parentOid o))]
                  [(:oid o) (oid->ytunnus (:parentOid o))])]
      (if (seq uudet)
        (recur (into oid->ytunnus uudet) (remove #(contains? oid->ytunnus (:parentOid %)) oppilaitoskoodit))
        (do
          (doseq [oppilaitos oppilaitoskoodit]
            (log/warn "Oppilaitos ilman parenttia:" (:oppilaitoskoodi oppilaitos)))
          oid->ytunnus)))))

(defn ^:private ^:integration-api paivita-koulutustoimijat! [koodit]
  (let [koulutustoimijat (->> (koulutustoimija-arkisto/hae-kaikki-organisaatiopalvelulle)
                           (map-by :ytunnus))]
    (doseq [koodi (vals (map-by y-tunnus koodit)) ;; Poistetaan duplikaatit
            :let [uusi-kt (koodi->koulutustoimija koodi)
                  y-tunnus (:ytunnus uusi-kt)
                  vanha-kt (koulutustoimijan-kentat (get koulutustoimijat y-tunnus))]
            :when y-tunnus]
      (cond
        (nil? vanha-kt) (do
                          (log/info "Uusi koulutustoimija: " (:ytunnus uusi-kt))
                          (koulutustoimija-arkisto/lisaa! uusi-kt))
        (not= vanha-kt uusi-kt) (do
                                  (log/info "Muuttunut koulutustoimija: " (:ytunnus uusi-kt))
                                  (koulutustoimija-arkisto/paivita! y-tunnus uusi-kt))))))

(defn ^:private ^:integration-api paivita-oppilaitokset! [koodit koulutustoimijakoodit]
  (let [oid->ytunnus (generoi-oid->y-tunnus koulutustoimijakoodit koodit)
        oppilaitokset (->> (oppilaitos-arkisto/hae-kaikki)
                        (map-by :oppilaitoskoodi))]
    (doseq [koodi (vals (map-by :oppilaitosKoodi koodit)) ;; Poistetaan duplikaatit
            ;; Poistetaan oppilaitokset joille ei löydy koulutustoimijaa
            ;; Oppilaitoksella on oltava koulutustoimija
            :when (oid->ytunnus (:parentOid koodi))
            :let [oppilaitoskoodi (:oppilaitosKoodi koodi)
                  koulutustoimija (oid->ytunnus (:parentOid koodi))
                  vanha-oppilaitos (oppilaitoksen-kentat (get oppilaitokset oppilaitoskoodi))
                  uusi-oppilaitos (assoc (koodi->oppilaitos koodi)
                                         :koulutustoimija koulutustoimija)]]
      (cond
        (nil? vanha-oppilaitos) (do
                                  (log/info "Uusi oppilaitos: " (:oppilaitoskoodi uusi-oppilaitos))
                                  (oppilaitos-arkisto/lisaa! uusi-oppilaitos))
        (not= vanha-oppilaitos uusi-oppilaitos) (do
                                                  (log/info "Muuttunut oppilaitos: " (:oppilaitoskoodi uusi-oppilaitos))
                                                  (oppilaitos-arkisto/paivita! oppilaitoskoodi uusi-oppilaitos))))))


(defn ^:private ^:integration-api paivita-toimipaikat! [koodit oppilaitoskoodit koulutustoimijakoodit]
  (let [oid->oppilaitostunnus (into {} (for [o oppilaitoskoodit]
                                         [(:oid o) (:oppilaitosKoodi o)]))
        oid->ytunnus (generoi-oid->y-tunnus koulutustoimijakoodit oppilaitoskoodit)
        toimipaikat (->> (toimipaikka-arkisto/hae-kaikki)
                      (map-by :toimipaikkakoodi))]
    (doseq [koodi (vals (map-by :toimipistekoodi koodit)) ;; Poistetaan duplikaatit
            ;; Poistetaan toimipaikat joille ei löydy oppilaitosta tai koulutustoimijaa
            ;; Oppilaitoksella on oltava koulutustoimija, toimipaikalla on oltava oppilaitos
            :when (and (oid->oppilaitostunnus (:parentOid koodi))
                       (oid->ytunnus (:parentOid koodi)))
            :let [toimipaikkakoodi (:toimipistekoodi koodi)
                  oppilaitos (oid->oppilaitostunnus (:parentOid koodi))
                  vanha-toimipaikka (toimipaikan-kentat (get toimipaikat toimipaikkakoodi))
                  uusi-toimipaikka (assoc (koodi->toimipaikka koodi)
                                          :oppilaitos oppilaitos)]]
      (cond
        (nil? vanha-toimipaikka) (do
                                   (log/info "Uusi toimipaikka: " (:toimipaikkakoodi uusi-toimipaikka))
                                   (toimipaikka-arkisto/lisaa! uusi-toimipaikka))
        (not= vanha-toimipaikka uusi-toimipaikka) (do
                                                    (log/info "Muuttunut toimipaikka: " (:toimipaikkakoodi uusi-toimipaikka))
                                                    (toimipaikka-arkisto/paivita! toimipaikkakoodi uusi-toimipaikka))))))

(defn ^:integration-api paivita-organisaatiot!
  [asetukset]
  (log/info "Aloitetaan organisaatioiden päivitys organisaatiopalvelusta")
  (let [kaikki-koodit (hae-kaikki (get asetukset "url"))
        koodit (group-by tyyppi kaikki-koodit)
        _ (log/info "Haettu kaikki organisaatiot")
        koulutustoimijakoodit (:koulutustoimija koodit)
        oppilaitoskoodit (:oppilaitos koodit)
        toimipaikkakoodit (:toimipaikka koodit)]
    (paivita-koulutustoimijat! koulutustoimijakoodit)
    (paivita-oppilaitokset! oppilaitoskoodit koulutustoimijakoodit)
    (paivita-toimipaikat! toimipaikkakoodit oppilaitoskoodit koulutustoimijakoodit)))
