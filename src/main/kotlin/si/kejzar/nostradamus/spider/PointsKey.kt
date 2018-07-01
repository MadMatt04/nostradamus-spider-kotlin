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
data class PointsKey(
        @PrimaryKeyColumn(value = "tournament_id", type = PrimaryKeyType.PARTITIONED) var tournamentId : UUID,
        @PrimaryKeyColumn var id : UUID = UUID.randomUUID(),
        @PrimaryKeyColumn(value ="user_id") var userId: UUID,
        @PrimaryKeyColumn("sync_id", type = PrimaryKeyType.PARTITIONED) var syncId : UUID,
        @PrimaryKeyColumn var position : Int = -1
)