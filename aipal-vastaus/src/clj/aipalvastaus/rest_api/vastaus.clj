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

(ns aipalvastaus.rest-api.vastaus
  (:require [compojure.core :as c]
            [korma.db :as db]
            [schema.core :as schema]
            [oph.common.util.http-util :refer [json-response-nocache]]
            [aipalvastaus.sql.vastaus :as vastaus]
            [aipalvastaus.sql.kyselykerta :as kysely]
            [oph.common.util.util :refer [map-by]]))

(defn validoi-vastaukset
  [vastaukset kysymykset]
  (when (every? true? (let [kysymysid->kysymys (map-by :kysymysid kysymykset)]
                        (for [vastaus vastaukset]
                          (when (kysymysid->kysymys (:kysymysid vastaus)) true))))
    vastaukset))

(defn muodosta-tallennettavat-vastaukset
  [vastaukset kysymykset]
  (flatten (let [kysymysid->kysymys (map-by :kysymysid kysymykset)]
             (for [vastaus vastaukset
                   :let [vastauksen-kysymys (kysymysid->kysymys (:kysymysid vastaus))
                         vastaustyyppi (:vastaustyyppi vastauksen-kysymys)
                         vastaus-arvot (if (vector? (:vastaus vastaus))
                                         (:vastaus vastaus)
                                         (vector (:vastaus vastaus)))]
                   :when vastauksen-kysymys]
               (for [arvo vastaus-arvot]
                 {:kysymysid (:kysymysid vastaus)
                  :vastaajaid 3679
                  :vastaustyyppi (:vastaustyyppi vastauksen-kysymys)
                  :numerovalinta (when (or (= "monivalinta" vastaustyyppi) (= "asteikko" vastaustyyppi)) arvo)
                  :vapaateksti (when (= "vapaateksti" vastaustyyppi) arvo)
                  :vaihtoehto (when (= "kylla_ei_valinta" vastaustyyppi) arvo)})))))

(defn tallenna-vastaukset!
  [vastaukset]
  (for [v vastaukset]
    (vastaus/tallenna! v)))

(defn validoi-ja-tallenna-vastaukset
  [vastaukset kysymykset]
  (-> vastaukset
    (validoi-vastaukset kysymykset)
    (muodosta-tallennettavat-vastaukset kysymykset)
    tallenna-vastaukset!
    (when "OK")))

(c/defroutes reitit
  (c/POST "/:vastaustunnus" [vastaustunnus vastaukset]
          (db/transaction
            (json-response-nocache
              (validoi-ja-tallenna-vastaukset vastaukset (kysely/hae-kysymykset vastaustunnus))))))
