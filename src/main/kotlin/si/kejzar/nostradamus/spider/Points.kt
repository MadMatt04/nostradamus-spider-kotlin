package si.kejzar.nostradamus.spider

import org.springframework.data.cassandra.core.mapping.Column
import org.springframework.data.cassandra.core.mapping.PrimaryKey
import org.springframework.data.cassandra.core.mapping.Table
import java.util.*

/**
 * @author matijak
 * @since 27/06/2018
 */
//CREATE TABLE IF NOT EXISTS points (
//id uuid,
//user_id uuid,
//sync_id uuid,
//total int,
//change int,
//matches int,
//position int,
//position_change int,
//percentage double,
//PRIMARY KEY (id, user_id, sync_id)
//);
@Table("points")
data class Points(
        @PrimaryKey var pointsKey: PointsKey,
        var total : Int,
        var change : Int,
        var matches : Int,
        var position: Int,
        @Column("position_change") var positionChange : Int,
        var percentage : Double
        )