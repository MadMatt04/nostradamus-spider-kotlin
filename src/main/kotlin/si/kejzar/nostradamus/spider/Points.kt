package si.kejzar.nostradamus.spider

import com.google.common.hash.Hashing
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
        var total : Int = -1,
        var change : Int = -1,
        var matches : Int = -1,
        @Column("position_change") var positionChange : Int = 0,
        var percentage : Double = -1.0
        )
{
        fun isEmpty(): Boolean {
            return total == -1 && change == -1 && matches == -1 && pointsKey.position == -1 && positionChange == 0 && percentage == -1.0
        }

        fun valueHash() : Int {
            val hashFunction = Hashing.goodFastHash(32)
            val hasher = hashFunction.newHasher()

            return hasher
                    .putInt(pointsKey.position)
                    .putInt(total)
                    .hash()
                    .asInt()
        }
}