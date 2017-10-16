-- :name lisaa-kysymys! :<!
INSERT INTO kysymys (pakollinen, poistettava, vastaustyyppi, kysymysryhmaid, kysymys_fi, kysymys_sv, kysymys_en, jarjestys, monivalinta_max, max_vastaus, eos_vastaus_sallittu, jatkokysymys)
    VALUES (:pakollinen, :poistettava, :vastaustyyppi, :kysymysryhmaid, :kysymys_fi, :kysymys_sv, :kysymys_en, :jarjestys, :monivalinta_max, :max_vastaus, :eos_vastaus_sallittu, :jatkokysymys)
    RETURNING kysymysid;

-- :name liita-jatkokysymys! :! :n
INSERT INTO kysymys_jatkokysymys(kysymysid, jatkokysymysid, vastaus) VALUES (:kysymysid, :jatkokysymysid, :vastaus);

-- :name hae-kysymykset :? :*
SELECT k.*, jk.kysymysid AS jatkokysymys_kysymysid, jk.vastaus AS jatkokysymys_vastaus FROM kysymys k
    LEFT JOIN kysymys_jatkokysymys jk ON k.kysymysid = jk.jatkokysymysid WHERE k.kysymysryhmaid = :kysymysryhmaid;

-- :name hae-kysymysryhma :? :*
SELECT * FROM kysymysryhma WHERE kysymysryhmaid = :kysymysryhmaid;