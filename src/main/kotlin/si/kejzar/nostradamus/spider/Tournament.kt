package si.kejzar.nostradamus.spider

import org.springframework.data.cassandra.core.mapping.Column
import org.springframework.data.cassandra.core.mapping.Indexed
import org.springframework.data.cassandra.core.mapping.PrimaryKey
import org.springframework.data.cassandra.core.mapping.Table
import java.io.Serializable
import java.util.*

/**
 * @author matijak
 * @since 20/06/2018
 */
@Table("tournament")
data class Tournament(
        @PrimaryKey var id: UUID = UUID.randomUUID(),
        var name: String,
        @Column("users_imported") @Indexed("si_tournament_ui") var usersImported: Boolean,
        @Column("all_matches") var allMatches: Int,
        @Column("low_score_matches") var lowScoreMatches: Int,
        @Column("high_score_matches") var highScoreMatches: Int,
        @Column("current_match") var currentMatch: Int,
        @Column("low_score_max_pts") var lowScoreMaxPts: Int,
        @Column("high_score_max_pts") var highScoreMaxPts: Int

) : Serializable