package si.kejzar.nostradamus.spider

import org.springframework.data.cassandra.repository.ReactiveCassandraRepository
import org.springframework.data.repository.CrudRepository
import java.util.*

/**
 * @author matijak
 * @since 14/06/2018
 */
interface TournamentUserRepository : ReactiveCassandraRepository<TournamentUser, TournamentUserKey> {
}