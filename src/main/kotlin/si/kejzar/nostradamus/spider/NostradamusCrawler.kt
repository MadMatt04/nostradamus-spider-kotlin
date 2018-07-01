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
import java.time.LocalDateTime
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
        @Autowired val tournamentRepository: TournamentRepository,
        @Autowired val pointsRepository: PointsRepository
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

        var syncAttempt = SyncAttempt(
                syncKey = SyncKey(tournamentId = tournamentId),
                attemptTime = LocalDateTime.now()
        )

        val usersToFind = tournamentUserRepository.findByTournamentUserKey_TournamentId(tournamentId)
                .map { tu -> tu.tournamentUserKey.userId }
                .flatMap { userId -> userRepository.findById(userId) }
//                .collectList()

        val latestSuccesfulAtt = syncAttemptRepository.findFirstBySyncKey_TournamentIdAndStatusOrderBySyncKey_AttemptNumberDesc(tournamentId, AttemptStatus.SUCCESSFUL)

        tournamentRepository.findById(tournamentId)
                .flatMap{ t : Tournament ->
                    syncAttemptRepository.findFirstBySyncKey_TournamentIdOrderBySyncKey_AttemptNumberDesc(t.id)
                            .switchIfEmpty(Mono.just(syncAttempt))
                            .map {
                                lastSync ->
                                Tuples.of(t, lastSync!!)
                            }
                }
                .doOnNext { tt ->
                    syncAttempt.syncKey.tournamentId = tt.t1.id
                    syncAttempt.matchNumberAfter = tt.t1.currentMatch + 1
                    syncAttempt.syncKey.attemptNumber = tt.t2.syncKey.attemptNumber + 1
                }
                .flatMap { tt ->
                    syncAttemptRepository.insert(syncAttempt)
                            .map { sa2 ->
                                syncAttempt = sa2
                                Tuples.of(tt.t1, tt.t2, sa2)
                            }
                }
                .flatMapMany { tuple ->
                    Flux.range(0, pages)
                            .map { pageNum -> Tuples.of(tuple.t1, pageNum) }
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
                .flatMap { contentTuple ->
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
                        val points = Points(pointsKey = PointsKey(tournamentId = tournamentId, userId = user.id, syncId = syncAttempt.syncKey.id))
                        val m = regex.matcher(content)
                        if (m.find()) {
                            val ranking = Integer.parseInt(m.group(1))
                            val score = Integer.parseInt(m.group(2))
                            points.total = score
                            points.pointsKey.position = ranking
                            logger.info("FOUND {}, pts: $score, ranking: $ranking", user.userName)
                        }

                        points
                    }
                            .filter{points -> !points.isEmpty()}

                }
                .flatMap { points ->
                    latestSuccesfulAtt.flatMap {
                        lsa ->
                        if (lsa == null) {
                            return@flatMap Mono.just(Tuples.of(points, 0))
                        }
                        pointsRepository.findByPointsKey_TournamentIdAndPointsKey_SyncIdAndPointsKey_UserId(
                                points.pointsKey.tournamentId,
                                lsa.syncKey.id,
                                points.pointsKey.userId
                        )
                                .map { prevPoints ->
                                    if (prevPoints != null) {
                                        points.positionChange = points.pointsKey.position - prevPoints.pointsKey.position
                                        points.change = points.total - prevPoints.total
                                    }

                                    Tuples.of(points, lsa.parseHash)
                                }
                    }

                }
                .doOnNext { ptsTuple ->
                    val points = ptsTuple.t1
                    if (points.isEmpty()) {
                        points.percentage = 0.0
                        points.positionChange = 0
                        points.change = 0
                    } }
                .doOnNext { ptsTuple -> hasher.putInt(ptsTuple.t1.valueHash()) }
                .collectList()
                .flatMapMany {
                    list ->
                    if (list.isEmpty() || list[0].t2 == hasher.hash().hashCode()) {
                        return@flatMapMany Flux.empty<Points>()
                    }

                    Flux.fromIterable(list)
                            .map { tuple -> tuple.t1 }
                }
                .flatMap { points -> pointsRepository.insert(points) }
                .subscribe { points ->
                    logger.info("Saved points {}", points)
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