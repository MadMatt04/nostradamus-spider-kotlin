package si.kejzar.nostradamus.spider

import org.springframework.data.cassandra.repository.ReactiveCassandraRepository
import org.springframework.data.repository.CrudRepository
import reactor.core.publisher.Flux
import java.util.*

/**
 * @author matijak
 * @since 14/06/2018
 */
interface TournamentUserRepository : ReactiveCassandraRepository<TournamentUser, TournamentUserKey> {

    fun findByTournamentUserKey_TournamentId(tournamentId : UUID) : Flux<TournamentUser>
}