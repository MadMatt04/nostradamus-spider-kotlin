CREATE KEYSPACE IF NOT EXISTS nostradamus
           WITH replication = {'class': 'SimpleStrategy', 'replication_factor' : 1};

           CREATE TABLE IF NOT EXISTS user (
               id uuid PRIMARY KEY,
               name text,
               userName text,
               emailAddress text
           );

           CREATE TABLE IF NOT EXISTS tournament (
                id uuid PRIMARY KEY,
                name text,
                users_imported boolean,
                all_matches int,
                low_score_matches int,
                high_score_matches int,
                current_match int,
                low_score_max_pts int,
                high_score_max_pts int
           );

           CREATE INDEX si_tournament_ui
              ON tournament (users_imported);

