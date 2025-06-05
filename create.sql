CREATE TABLE license
(
    id serial PRIMARY KEY,
    lastmodifieddate time with time zone DEFAULT now(),
    licensename text,
    maxpage integer,
    maxmen integer,
    serverurl text
 );
 
CREATE TABLE organization
(
    id serial PRIMARY KEY,
    lastmodifieddate time with time zone DEFAULT now(),
    numberof integer,
    enddate date,
    status text,
    oem boolean,
    orgid text NOT NULL,
    startdate date NOT NULL,
    license integer NOT NULL
);

CREATE TABLE usertbl
(
    id serial PRIMARY KEY,
    lastmodifieddate time with time zone DEFAULT now(),
    userid text NOT NULL,
    organization integer NOT NULL,
    license integer
);

ALTER TABLE organization ADD FOREIGN KEY (license) REFERENCES license (id) ON DELETE SET NULL;
ALTER TABLE usertbl ADD FOREIGN KEY (license) REFERENCES license (id) ON DELETE SET NULL;
ALTER TABLE usertbl ADD FOREIGN KEY (organization) REFERENCES organization (id) ON DELETE CASCADE;


CREATE TABLE accesslog
(
    id serial PRIMARY KEY,
    lastmodifieddate time with time zone DEFAULT now(),
    lastdate timestamp without time zone,
    accesscnt integer,
    pageovercnt integer,
    memovercnt integer,
    idruningcnt integer,
    otherercnt integer,
    licensename text,
    memo text,
    lastjson text,
    ip text,
    datacheckcnt integer,
    v6cnt integer,
    userid text,
    orgid text,
    user integer
);
ALTER TABLE accesslog ADD FOREIGN KEY (user) REFERENCES user (id) ON DELETE SET NULL;



