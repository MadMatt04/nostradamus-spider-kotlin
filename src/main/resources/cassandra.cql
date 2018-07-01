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

           CREATE INDEX IF NOT EXISTS si_tournament_ui
              ON tournament (users_imported);


            CREATE TABLE IF NOT EXISTS tournament_user (
                tournament_id uuid,
                user_id uuid,
                PRIMARY KEY (tournament_id, user_id)
            );

            CREATE TABLE IF NOT EXISTS sync_attempt (
                id uuid,
                tournament_id uuid,
                attempt_number int,
                attempt_time timestamp,
                parse_hash int,
                match_number_after int,
                status text,
                PRIMARY KEY (tournament_id, attempt_number, id)
            );

            CREATE INDEX IF NOT EXISTS si_sync_attempt_status ON sync_attempt (status);



            CREATE TABLE IF NOT EXISTS points (
                id uuid,
                user_id uuid,
                sync_id uuid,
                tournament_id uuid,
                total int,
                change int,
                matches int,
                position int,
                position_change int,
                percentage double,
                PRIMARY KEY (tournament_id, sync_id, position, user_id, id)
            );

            CREATE INDEX IF NOT EXISTS si_points_si_total ON points (total);

