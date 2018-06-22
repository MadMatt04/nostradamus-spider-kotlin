package si.kejzar.nostradamus.spider

import org.springframework.data.cassandra.core.mapping.Column
import org.springframework.data.cassandra.core.mapping.PrimaryKey
import org.springframework.data.cassandra.core.mapping.Table
import java.util.*

/**
 * @author matijak
 * @since 22/06/2018
 */
@Table("tournament_user")
data class TournamentUser(@PrimaryKey var tournamentUserKey: TournamentUserKey)