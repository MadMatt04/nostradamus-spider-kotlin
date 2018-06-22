package si.kejzar.nostradamus.spider

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.CommandLineRunner
import org.springframework.core.io.ClassPathResource
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.util.function.Tuple2
import reactor.util.function.Tuples
import java.io.File
import java.util.*
import javax.annotation.PostConstruct

/**
 * @author matijak
 * @since 20/06/2018
 */
@Component
class UserImporter  @Autowired constructor(
        private val tournamentRepository: TournamentRepository,
        private val userRepository: UserRepository,
        private val tournamentUserRepository: TournamentUserRepository
)    {

    @PostConstruct
    fun importUsersIfNecessary() {

        getUsers()
                .flatMap {
                    user ->

                    userRepository.existsById(user.id)
                            .flatMap {
                                exists ->
                                if (!exists)
                                    userRepository.insert(user)
                                else
                                    Mono.just(user)

                            }
                }
                .collectList()
                .flatMapMany({
                    ul ->
                    getTournaments().map({
                        t -> Tuples.of(t, ul)
                    })
                })
                .flatMap {
                    tuple ->
                    Flux.fromIterable(tuple.t2)
                            .map { u -> Tuples.of(tuple.t1, u) }

                }
                .map {
                    tuple -> Tuples.of(tuple.t1, TournamentUser(TournamentUserKey(tuple.t1.id, tuple.t2.id)))
                }
                .flatMap {
                    tuple ->


                    tournamentUserRepository.existsById(tuple.t2.tournamentUserKey)
                            .flatMap {
                                exists ->
                                if (exists)
                                    Mono.just(tuple)
                                else
                                    tournamentUserRepository.insert(tuple.t2)
                                            .map {
                                                tu ->
                                                Tuples.of(tuple.t1, tu)
                            }
                        }
                }
                .map { tuple -> tuple.t1 }
                .distinct()
                .subscribe {
                    t ->
                    t.usersImported = true
                    tournamentRepository.save(t)
                }



    }

    private fun getTournaments() : Flux<Tournament> {

        return tournamentRepository.findByUsersImported(false)

    }

    private fun getUsers(): Flux<User> {
        val userCpRes = ClassPathResource("/users.csv")
        return Flux.fromIterable(userCpRes.file.readLines())
                .map({
                    userStr ->
                    userStr.split(',')
                })
                .map({
                    strList ->
                    User(UUID.fromString(strList[0]), strList[1], strList[2], strList[3])
                })
    }

}