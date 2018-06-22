package si.kejzar.nostradamus.spider

import org.springframework.data.cassandra.core.cql.PrimaryKeyType
import org.springframework.data.cassandra.core.mapping.Column
import org.springframework.data.cassandra.core.mapping.PrimaryKeyClass
import org.springframework.data.cassandra.core.mapping.PrimaryKeyColumn
import java.util.*

/**
 * @author matijak
 * @since 22/06/2018
 */
@PrimaryKeyClass
data class TournamentUserKey(
        @PrimaryKeyColumn("tournament_id") var tournamentId : UUID,
        @PrimaryKeyColumn(value ="user_id", type = PrimaryKeyType.PARTITIONED) var userId: UUID
)