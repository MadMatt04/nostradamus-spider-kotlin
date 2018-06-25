package si.kejzar.nostradamus.spider

import org.springframework.data.cassandra.repository.ReactiveCassandraRepository
import org.springframework.data.repository.CrudRepository
import reactor.core.publisher.Mono
import java.util.*

/**
 * @author matijak
 * @since 14/06/2018
 */
interface SyncAttemptRepository : ReactiveCassandraRepository<SyncAttempt, UUID> {

    fun findTop1ByAttemptNumber() : Mono<SyncAttempt>
}