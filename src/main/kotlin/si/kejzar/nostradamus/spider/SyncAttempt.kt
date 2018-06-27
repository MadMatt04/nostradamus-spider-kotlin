package si.kejzar.nostradamus.spider

import org.springframework.data.cassandra.core.mapping.Column
import org.springframework.data.cassandra.core.mapping.PrimaryKey
import org.springframework.data.cassandra.core.mapping.Table
import java.time.ZonedDateTime
import java.util.*

/**
 * @author matijak
 * @since 22/06/2018
 */
@Table("sync_attempt")
data class SyncAttempt(
        @PrimaryKey var id: UUID = UUID.randomUUID(),
        @Column("tournament_id") var tournamentId : UUID,
        @Column("attempt_number") var attemptNumber: Int = 0,
        @Column("attempt_time") var attemptTime: ZonedDateTime = ZonedDateTime.now(),
        @Column("parse_hash") var parseHash: Int = 0,
        @Column("match_number_after") var matchNumberAfter: Int = 0,
        var status: AttemptStatus = AttemptStatus.SUCCESSFUL
)