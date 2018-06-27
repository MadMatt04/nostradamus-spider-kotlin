package si.kejzar.nostradamus.spider

import com.google.common.hash.Hashing
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.util.function.Tuples
import java.time.ZonedDateTime
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.regex.Pattern

/**
 * @author matijak
 * @since 22/06/2018
 */
@Component
@EnableScheduling
class NostradamusCrawler(
        @Value("\${baseUrl}") val baseUrl: String,
        @Value("\${page}") val page: String,
        @Value("\${pages}") val pages: Int,
        @Autowired val tournamentUserRepository: TournamentUserRepository,
        @Autowired val userRepository: UserRepository,
        @Autowired val syncAttemptRepository: SyncAttemptRepository,
        @Autowired val tournamentRepository: TournamentRepository
) {
    private val logger: Logger = LoggerFactory.getLogger(NostradamusCrawler::class.java)
    // TODO move this to DB
    private val tournamentId = UUID.fromString("b7e159a6-8590-4d72-b18d-12f1d283924d")

    private val leaderPattern: Pattern = Pattern.compile("<td class=\"tac\">1\\\\.</td>\\n\\\\s*\\n\\\\s*<td><a href=\"/profil/\\\\S+\">(.+)</a></td>\\n\\\\s+<td class=\"tac\">(\\\\d+)</td>")

    private val regexMap: MutableMap<User, Pattern> = ConcurrentHashMap()

    @Scheduled(fixedRate = 360000L)
    fun crawl() {
        val webClient = WebClient.builder().baseUrl(baseUrl).build()
        val hashFunction = Hashing.crc32()
        val hasher = hashFunction.newHasher()

        val reqTime = ZonedDateTime.now()

        var syncAttempt = SyncAttempt(
                tournamentId = tournamentId,
                attemptTime = ZonedDateTime.now()
        )

        val usersToFind = tournamentUserRepository.findByTournamentUserKey_TournamentId(tournamentId)
                .map { tu -> tu.tournamentUserKey.userId }
                .flatMap { userId -> userRepository.findById(userId) }
//                .collectList()

        tournamentRepository.findById(tournamentId)
                .flatMap{ t : Tournament ->
                    syncAttemptRepository.findFirstByTournamentIdOrderByAttemptNumber(t.id)
                            .switchIfEmpty(Mono.just(SyncAttempt(tournamentId = t.id, attemptTime = ZonedDateTime.now().withYear(1970))))
                            .map {
                                lastSync ->
                                Tuples.of(t, lastSync!!)
                            }
                }
                .doOnNext { tt ->
                    syncAttempt.tournamentId = tt.t1.id
                    syncAttempt.matchNumberAfter = tt.t1.currentMatch - 1
                    syncAttempt.attemptNumber = tt.t2.attemptNumber + 1
                }
                .flatMapMany { t ->
                    Flux.range(0, pages)
                            .map { pageNum -> Tuples.of(t, pageNum) }
                }
                .map { pageNumTuple ->
                    Tuples.of(pageNumTuple.t1, pageNumTuple.t2, webClient.get().uri { uri ->
                        uri.queryParam(page, pageNumTuple.t2).build()
                    })
                }
                .doOnNext { tuple -> logger.info("Requesting page {}", tuple.t2) }

                .flatMap { specTuple -> specTuple.t3.exchange().map { response -> Tuples.of(specTuple.t1, specTuple.t2, response) } }

                .doOnNext { tuple -> if (tuple.t3.statusCode() != HttpStatus.OK) throw IllegalStateException("Non-OK response received") }
//                .flatMap {
//                    tuple ->
//                    syncAttemptRepository.findFirstByTournamentIdOrderByAttemptNumber()
//                            .map {
//                                lastSync ->  SyncAttempt(attemptNumber = lastSync.attemptNumber + 1, attemptTime = reqTime, status = AttemptStatus.SUCCESSFUL, parseHash = 0)
//                            }
//                            .map { newSync -> Tuples.of(tuple.t1, tuple.t2, newSync) }
//                }
                .flatMap { tuple -> tuple.t3.bodyToMono(String::class.java).map { s -> Tuples.of(tuple.t1, tuple.t2, s) } }
                .doOnNext { tuple -> logger.info("Read page {}", tuple.t2) }
                .subscribe { contentTuple ->
                    val content = contentTuple.t3
                    if (contentTuple.t2 == 0) {
                        val m = leaderPattern.matcher(content)
                        if (m.find()) {
                            val username = m.group(1)
                            val score = Integer.parseInt(m.group(2))
                        }
                    }

//                    val users = tournamentUserRepository.findByTournamentUserKey_TournamentId(contentTuple.t1.id)
//                            .map { tu -> tu.tournamentUserKey.userId }
//                            .flatMap { userId ->  userRepository.findById(userId) }
//                            .collectList()


                    usersToFind.map { user ->

                        logger.info("Looking for user {}, page {}..", user.userName, contentTuple.t2)
                        val regex = regexForUser(user)
                        val m = regex.matcher(content)
                        if (m.find()) {
                            val ranking = Integer.parseInt(m.group(1))
                            val score = Integer.parseInt(m.group(2))
                            logger.info("FOUND {}, pts: $score, ranking: $ranking", user.userName)
                        } else {
                            logger.info("{} not found, page {}", user.userName, user.userName)
                        }



                    }
                }


    }

    private fun regexForUser(user: User): Pattern {
        var regex: Pattern? = regexMap.get(user)
        if (regex == null) {
            regex = Pattern.compile("<td class=\"tac\">(\\d+)\\.</td>\n\\s*\n\\s*<td><a href=\"/profil/\\S+\">" + user
                    .userName + "</a></td>\n\\s+<td class=\"tac\">(\\d+)</td>")
            regexMap[user] = regex
        }

        return regex!!
    }
}