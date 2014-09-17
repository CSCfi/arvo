(ns aipal.infra.kayttaja.vaihto-test
  (:require [clojure.test :refer :all]
            [aipal.sql.test-util :refer [tietokanta-fixture]]
            [aipal.infra.kayttaja :refer [*kayttaja*]]
            [aipal.infra.kayttaja.vaihto :refer :all]
            [aipal.arkisto.kayttaja :as kayttaja-arkisto]
            [aipal.arkisto.kayttajaoikeus :as kayttajaoikeus-arkisto]))

(use-fixtures :each tietokanta-fixture)

;; with-kayttaja heittää IllegalStateExceptionin jos UIDilla ei löydy
;; voimassaolevaa käyttäjää.
(deftest with-kayttaja-ei-voimassaolevaa-kayttajaa
  (is (thrown? IllegalStateException (with-kayttaja "uid" nil))))

;; Jos UIDilla löytyy voimassaoleva käyttäjä, with-kayttaja ajaa annetun koodin
;; sitoen varin *kayttaja* käyttäjän tietoihin.
(deftest with-kayttaja-sidonta
  (let [k (atom nil)]
    (with-redefs [kayttaja-arkisto/hae-voimassaoleva (constantly {:oid "oid"})]
      (with-kayttaja "uid" nil
        (reset! k *kayttaja*))
      (is (= (:oid @k) "oid")))))

;; Impersonoinnin aikana voimassaoleva OID = impersonoitavan käyttäjän OID.
(deftest with-kayttaja-voimassaoleva-oid-impersonointi
  (let [k (atom nil)]
    (with-redefs [kayttaja-arkisto/hae-voimassaoleva (constantly {:oid "oid"})]
      (with-kayttaja "uid" "impersonoitava-oid"
        (reset! k *kayttaja*))
      (is (= (:voimassaoleva-oid @k) "impersonoitava-oid")))))

;; Impersonoinnin aikana voimassaolevat roolit = impersonoitavan käyttäjän roolit.
(deftest with-kayttaja-voimassaolevat-roolit-impersonointi
  (let [k (atom nil)]
    (with-redefs [kayttaja-arkisto/hae-voimassaoleva (constantly {:oid "oid"})
                  kayttajaoikeus-arkisto/hae-roolit
                  {"oid" :...omat-roolit...
                   "impersonoitava-oid" :...impersonoidut-roolit...}]
      (with-kayttaja "uid" "impersonoitava-oid"
        (reset! k *kayttaja*))
      (is (= (:voimassaolevat-roolit @k) :...impersonoidut-roolit...)))))

;; Ilman impersonointia voimassaoleva OID = käyttäjän oma OID.
(deftest with-kayttaja-voimassaoleva-oid-ei-impersonointia
  (let [k (atom nil)]
    (with-redefs [kayttaja-arkisto/hae-voimassaoleva (constantly {:oid "oid"})]
      (with-kayttaja "uid" nil
        (reset! k *kayttaja*))
      (is (= (:voimassaoleva-oid @k) "oid")))))

;; Ilman impersonointia voimassaolevat roolit = käyttäjän omat roolit.
(deftest with-kayttaja-voimassaolevat-roolit-ei-impersonointia
  (let [k (atom nil)]
    (with-redefs [kayttaja-arkisto/hae-voimassaoleva (constantly {:oid "oid"})
                  kayttajaoikeus-arkisto/hae-roolit
                  {"oid" :...omat-roolit...
                   "impersonoitava-oid" :...impersonoidut-roolit...}]
      (with-kayttaja "uid" nil
        (reset! k *kayttaja*))
      (is (= (:voimassaolevat-roolit @k) :...omat-roolit...)))))

;; with-kayttaja muodostaa käyttäjän koko nimen
(deftest with-kayttaja-nimi
  (let [k (atom nil)]
    (with-redefs [kayttaja-arkisto/hae-voimassaoleva
                  (constantly {:etunimi "Matti"
                               :sukunimi "Meikäläinen"})]
      (with-kayttaja "uid" nil
        (reset! k *kayttaja*))
      (is (= (:nimi @k) "Matti Meikäläinen")))))
