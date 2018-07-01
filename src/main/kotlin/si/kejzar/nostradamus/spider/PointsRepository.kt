package si.kejzar.nostradamus.spider

import org.springframework.data.cassandra.repository.ReactiveCassandraRepository
import org.springframework.data.repository.CrudRepository
import reactor.core.publisher.Mono
import java.util.*

/**
 * @author matijak
 * @since 14/06/2018
 */
interface PointsRepository : ReactiveCassandraRepository<Points, UUID> {


    fun findByPointsKey_TournamentIdAndPointsKey_SyncIdAndPointsKey_UserId(tournamentId: UUID, syncId: UUID, userId: UUID) : Mono<Points?>


}