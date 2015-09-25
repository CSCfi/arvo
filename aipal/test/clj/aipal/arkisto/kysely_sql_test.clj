(ns aipal.arkisto.kysely-sql-test
  (:require [clojure.test :refer :all]
            [aipal.arkisto.kysely :refer :all]
            [aipal.sql.test-data-util :as test-data]
            [aipal.sql.test-util :refer [tietokanta-fixture]]))

(use-fixtures :each tietokanta-fixture)

(deftest ^:integraatio hae-ntm-kyselyt-test
  (let [kysymysryhma     (test-data/lisaa-kysymysryhma! {:ntm_kysymykset true
                                                         :taustakysymykset false
                                                         :tila "julkaistu"
                                                         :valtakunnallinen true})
        kysely           (test-data/lisaa-kysely!)
        _                (test-data/lisaa-kysymysryhma-kyselyyn! kysymysryhma kysely)
        sisaltaa-kyselyn (fn [kyselyt kyselyid]
                           (some #{kyselyid}
                                 (map :kyselyid kyselyt)))]
    (testing
      "pääkäyttäjä näkee NTM-kyselyn"
      (with-redefs [aipal.infra.kayttaja/yllapitaja? (constantly true)]
        (is (sisaltaa-kyselyn (hae-kyselyt (:koulutustoimija kysely))
                              (:kyselyid kysely)))))
    (testing
      "NTM-vastuukäyttäjä näkee NTM-kyselyn"
      (with-redefs [aipal.infra.kayttaja/yllapitaja? (constantly false)
                    aipal.infra.kayttaja/ntm-vastuukayttaja? (constantly true)]
        (is (sisaltaa-kyselyn (hae-kyselyt (:koulutustoimija kysely))
                              (:kyselyid kysely)))))
    (testing
      "tavallinen käyttäjä ei näe NTM-kyselyä"
      (with-redefs [aipal.infra.kayttaja/yllapitaja? (constantly false)
                    aipal.infra.kayttaja/ntm-vastuukayttaja? (constantly false)]
        (is (not (sisaltaa-kyselyn (hae-kyselyt (:koulutustoimija kysely))
                                   (:kyselyid kysely))))))))