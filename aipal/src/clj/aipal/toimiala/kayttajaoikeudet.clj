(ns aipal.toimiala.kayttajaoikeudet
  "https://knowledge.solita.fi/pages/viewpage.action?pageId=61901330"
  (:require [aipal.toimiala.kayttajaroolit :refer :all]
            [aipal.arkisto.kayttajaoikeus :as kayttajaoikeus-arkisto]
            [aipal.arkisto.kysely :as kysely-arkisto]
            [aipal.arkisto.kyselykerta :as kyselykerta-arkisto]
            [aipal.arkisto.kysymysryhma :as kysymysryhma-arkisto]
            [aipal.arkisto.kyselypohja :as kyselypohja-arkisto]
            [aipal.infra.kayttaja :refer [*kayttaja*]]))

(defn ->int
  "Merkkijono numeroksi tai numero sellaisenaan."
  [str-or-int]
  {:pre [(or (integer? str-or-int)
             (string? str-or-int))]
   :post [(integer? %)]}
  (if (string? str-or-int)
    (Integer/parseInt str-or-int)
    str-or-int))

(defn aipal-kayttaja?
  ([x] (aipal-kayttaja?))
  ([]
    true))

(defn sisaltaa-jonkin-rooleista? [roolit roolirivit]
  (not (empty? (clojure.set/select roolit (set (map :rooli roolirivit))))))

(defn kayttajalla-on-jokin-rooleista? [roolit]
  (sisaltaa-jonkin-rooleista? roolit (:aktiiviset-roolit *kayttaja*)))

(defn kayttajalla-on-jokin-rooleista-koulutustoimijassa? [roolit koulutustoimija]
  (let [roolit-koulutustoimijassa (filter #(= koulutustoimija (:organisaatio %)) (:aktiiviset-roolit *kayttaja*))]
    (sisaltaa-jonkin-rooleista? roolit roolit-koulutustoimijassa)))

(defn kayttajalla-on-jokin-rooleista-kyselyssa? [roolit kyselyid]
  (let [koulutustoimija (:koulutustoimija (kysely-arkisto/hae-organisaatiotieto (->int kyselyid)))]
    (kayttajalla-on-jokin-rooleista-koulutustoimijassa? roolit koulutustoimija)))

(defn kayttajalla-on-jokin-rooleista-kysymysryhmassa? [roolit kysymysryhmaid]
  (let [koulutustoimija (:koulutustoimija (kysymysryhma-arkisto/hae-organisaatiotieto (->int kysymysryhmaid)))]
    (kayttajalla-on-jokin-rooleista-koulutustoimijassa? roolit koulutustoimija)))

(defn kayttajalla-on-jokin-rooleista-kyselypohjassa? [roolit kyselypohjaid]
  (let [koulutustoimija (:koulutustoimija (kyselypohja-arkisto/hae-organisaatiotieto (->int kyselypohjaid)))]
    (kayttajalla-on-jokin-rooleista-koulutustoimijassa? roolit koulutustoimija)))

(defn hae-kyselyn-tila [kyselyid]
  (:tila (kysely-arkisto/hae (->int kyselyid))))

(defn kysely-on-luonnostilassa? [kyselyid]
  (= "luonnos" (hae-kyselyn-tila kyselyid)))

(defn kysely-on-julkaistu? [kyselyid]
  (= "julkaistu" (hae-kyselyn-tila kyselyid)))

(defn kayttajalla-on-lukuoikeus-kysymysryhmaan? [kysymysryhmaid]
  (let [organisaatiotieto (kysymysryhma-arkisto/hae-organisaatiotieto (->int kysymysryhmaid))]
    (or (:valtakunnallinen organisaatiotieto)
        (= (:koulutustoimija organisaatiotieto) (:aktiivinen-koulutustoimija *kayttaja*)))))

(defn kayttajalla-on-lukuoikeus-kyselypohjaan? [kyselypohjaid]
  (let [organisaatiotieto (kyselypohja-arkisto/hae-organisaatiotieto (->int kyselypohjaid))]
    (or (:valtakunnallinen organisaatiotieto)
        (= (:koulutustoimija organisaatiotieto) (:aktiivinen-koulutustoimija *kayttaja*)))))

(defn yllapitaja? []
  (kayttajalla-on-jokin-rooleista?
    #{"YLLAPITAJA"}))

(defn paakayttaja-tai-vastuukayttaja? []
  (kayttajalla-on-jokin-rooleista?
    #{"OPL-PAAKAYTTAJA"
      "OPL-VASTUUKAYTTAJA"}))

(defn impersonoiva-yllapitaja? []
  (not= (:oid *kayttaja*) (:aktiivinen-oid *kayttaja*)))

(defn kyselyiden-listaaminen?
  "Onko kyselyiden listaaminen sallittua yleisesti toimintona?"
  []
  (or (yllapitaja?)
      (paakayttaja-tai-vastuukayttaja?)
      (kayttajalla-on-jokin-rooleista?
        #{"OPL-KAYTTAJA"
          "OPL-KATSELIJA"})))

(defn kysely-luonti? []
  (or (yllapitaja?)
      (paakayttaja-tai-vastuukayttaja?)))

(defn kysely-muokkaus?
  "Onko kyselyn muokkaus sallittu."
  [kyselyid]
  (and (kysely-on-luonnostilassa? kyselyid)
       (or (yllapitaja?)
           (kayttajalla-on-jokin-rooleista-kyselyssa?
             #{"OPL-PAAKAYTTAJA"
               "OPL-VASTUUKAYTTAJA"}
             kyselyid))))

(defn kysely-tilamuutos?
  "Onko kyselyn tilan muutos (luonnos/julkaistu/suljettu) sallittu."
  [kyselyid]
  (or (yllapitaja?)
      (kayttajalla-on-jokin-rooleista-kyselyssa? #{"OPL-PAAKAYTTAJA"
                                                   "OPL-VASTUUKAYTTAJA"} kyselyid)))

(defn kysely-luku? [kyselyid]
  (or (yllapitaja?)
      (kayttajalla-on-jokin-rooleista-kyselyssa?
        #{"OPL-PAAKAYTTAJA"
          "OPL-VASTUUKAYTTAJA"
          "OPL-KAYTTAJA"
          "OPL-KATSELIJA"}
        kyselyid)))

(defn kyselykerta-luku? [kyselykertaid]
  (let [kyselykerta (kyselykerta-arkisto/hae-yksi (->int kyselykertaid))]
    (kysely-luku? (:kyselyid kyselykerta))))

(defn kysymysryhma-listaaminen? []
  (or (yllapitaja?)
      (paakayttaja-tai-vastuukayttaja?)
      (kayttajalla-on-jokin-rooleista?
        #{"OPL-KAYTTAJA"
          "OPL-KATSELIJA"})))

(defn kysymysryhma-luku? [kysymysryhmaid]
  (or (yllapitaja?)
      (kayttajalla-on-lukuoikeus-kysymysryhmaan? kysymysryhmaid)))

(defn kysymysryhma-luonti? []
  (or (yllapitaja?)
      (paakayttaja-tai-vastuukayttaja?)))

(defn kysymysryhma-on-luonnostilassa? [kysymysryhmaid]
  (= "luonnos" (:tila (kysymysryhma-arkisto/hae (->int kysymysryhmaid) false))))

(defn kysymysryhma-muokkaus? [kysymysryhmaid]
  (and (kysymysryhma-on-luonnostilassa? kysymysryhmaid)
       (or (yllapitaja?)
          (kayttajalla-on-jokin-rooleista-kysymysryhmassa?
            #{"OPL-PAAKAYTTAJA"
              "OPL-VASTUUKAYTTAJA"}
            kysymysryhmaid))))

(defn kyselypohja-muokkaus? [kyselypohjaid]
  (or (yllapitaja?)
      (kayttajalla-on-jokin-rooleista-kyselypohjassa?
        #{"OPL-PAAKAYTTAJA"
          "OPL-VASTUUKAYTTAJA"}
        kyselypohjaid)))

(defn kyselypohja-listaaminen? []
  (or (yllapitaja?)
      (paakayttaja-tai-vastuukayttaja?)))

(defn kyselypohja-luku? [kyselypohjaid]
  (or (yllapitaja?)
      (kayttajalla-on-lukuoikeus-kyselypohjaan? kyselypohjaid)))

(defn kyselykerta-luonti? [kyselyid]
  (and (kysely-on-julkaistu? kyselyid)
       (or (yllapitaja?)
           (kayttajalla-on-jokin-rooleista-kyselyssa?
             #{"OPL-PAAKAYTTAJA"
               "OPL-VASTUUKAYTTAJA"
               "OPL-KAYTTAJA"}
             kyselyid))))

(defn kyselykerta-muokkaus? [kyselykertaid]
  (let [kyselyid (kyselykerta-arkisto/kyselykertaid->kyselyid (->int kyselykertaid))]
    (kyselykerta-luonti? kyselyid)))

(def kayttajatoiminnot
  `{:logitus aipal-kayttaja?
    :kieli aipal-kayttaja?
    :vastaajatunnus aipal-kayttaja?
    :kysely kyselyiden-listaaminen?
    :kysely-luonti kysely-luonti?
    :kysely-luku kysely-luku?
    :kysely-muokkaus kysely-muokkaus?
    :kysely-tilamuutos kysely-tilamuutos?
    :kyselykerta-luku kyselykerta-luku?
    :kyselykerta-luonti kyselykerta-luonti?
    :kyselykerta-muokkaus kyselykerta-muokkaus?
    :kysymysryhma-listaaminen kysymysryhma-listaaminen?
    :kysymysryhma-luku kysymysryhma-luku?
    :kysymysryhma-luonti kysymysryhma-luonti?
    :kysymysryhma-muokkaus kysymysryhma-muokkaus?
    :kysymysryhma-julkaisu kysymysryhma-muokkaus?
    :kyselypohja-listaaminen kyselypohja-listaaminen?
    :kyselypohja-luku kyselypohja-luku?
    :kyselypohja-muokkaus kyselypohja-muokkaus?
    :impersonointi yllapitaja?
    :impersonointi-lopetus impersonoiva-yllapitaja?
    :kayttajan_tiedot aipal-kayttaja?
    :ohjeet_luku aipal-kayttaja?
    :ohje_muokkaus yllapitaja?
    :rahoitusmuoto aipal-kayttaja?
    :omat_tiedot aipal-kayttaja?})

(def toiminnot kayttajatoiminnot)
