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
data class SyncKey(
        @PrimaryKeyColumn var id : UUID = UUID.randomUUID(),
        @PrimaryKeyColumn(value ="tournament_id", type = PrimaryKeyType.PARTITIONED) var tournamentId: UUID,
        @PrimaryKeyColumn("attempt_number") var attemptNumber : Int = 0,
        @PrimaryKeyColumn var status: AttemptStatus = AttemptStatus.IN_PROGRESS
)