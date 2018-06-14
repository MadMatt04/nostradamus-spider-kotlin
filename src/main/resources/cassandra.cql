CREATE KEYSPACE IF NOT EXISTS nostradamus
           WITH replication = {'class': 'SimpleStrategy', 'replication_factor' : 1};

           CREATE TABLE IF NOT EXISTS user (
               id uuid PRIMARY KEY,
               name text,
               userName text,
               emailAddress text
           );