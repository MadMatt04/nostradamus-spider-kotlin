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
import reactor.util.function.component1
import java.math.MathContext
import java.math.RoundingMode
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

    private val percMathContext = MathContext(3, RoundingMode.HALF_UP)

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

        val latestSuccessfulAtt =
                syncAttemptRepository.findFirstBySyncKey_TournamentIdAndSyncKey_StatusOrderBySyncKey_StatusDescSyncKey_AttemptNumberDesc(tournamentId, AttemptStatus.SUCCESSFUL)
                        .switchIfEmpty(Mono.just(SyncAttempt(syncKey = SyncKey(tournamentId = tournamentId), attemptTime = LocalDateTime.MIN)))
                        .flux()

        val tournamentMono = tournamentRepository.findById(tournamentId)

        val calculatePossible = tournamentMono.map { t ->
            val lowPtMatches = minOf(t.lowScoreMatches, t.currentMatch)
            val hiPtMatches = maxOf(0, t.currentMatch - lowPtMatches)

            lowPtMatches * t.lowScoreMaxPts + hiPtMatches * t.highScoreMaxPts
        }

        tournamentMono
                .flatMap { t: Tournament ->
                    syncAttemptRepository.findFirstBySyncKey_TournamentIdOrderBySyncKey_StatusDescSyncKey_AttemptNumberDesc(t.id)
                            .switchIfEmpty(Mono.just(syncAttempt))
                            .map { lastSync ->
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
                            .filter { points -> !points.isEmpty() }

                }
//                .(latestSuccessfulAtt) // TODO only combines the one
                .flatMap { points ->

                    latestSuccessfulAtt.map { lsa ->
                        Tuples.of(points, lsa!!)
                    }
                }
                .flatMap { ptsLsaTuple ->
                    val points = ptsLsaTuple.t1
                    if (ptsLsaTuple.t2.syncKey.status != AttemptStatus.SUCCESSFUL) {
                        return@flatMap Mono.just(Tuples.of(points, 0))
                    } else {
                        val lsa = ptsLsaTuple.t2
                        return@flatMap pointsRepository.findByPointsKey_TournamentIdAndPointsKey_SyncIdAndPointsKey_UserId(
                                points.pointsKey.tournamentId,
                                lsa.syncKey.id,
                                points.pointsKey.userId
                        )
                                .map { prevPoints ->
                                    if (prevPoints != null) {
                                        points.positionChange = points.pointsKey.position - prevPoints.pointsKey.position
                                        points.change = points.total - prevPoints.total
                                    } else {

                                    }

                                    return@map Tuples.of(points, lsa.parseHash)
                                }
                    }
                }
                .doOnNext { ptsTuple ->
                    val points = ptsTuple.t1
                    if (points.isEmpty() || points.change < 0 || points.matches < -1 || points.percentage < -1) {
                        points.percentage = 0.0
                        points.positionChange = 0
                        points.change = 0
                    }
                }
                .doOnNext { ptsTuple -> hasher.putInt(ptsTuple.t1.valueHash()) }
                .collectList()
                .flatMapMany { list ->
                    if (list.isEmpty() || list[0].t2 == hasher.hash().hashCode()) {
                        return@flatMapMany Flux.empty<Points>()
                    }

                    Flux.fromIterable(list)
                            .map { tuple -> tuple.t1 }
                }
                .flatMap { points ->
                    latestSuccessfulAtt.zipWith(tournamentMono)
                            .map { ltaTour -> Tuples.of(points, ltaTour.component1()!!, ltaTour.t2) }
                            .filter { t3 -> t3.t2.parseHash != hasher.hash().asInt() }
                            .zipWith(calculatePossible)
                            .map { tTuple -> Tuples.of(tTuple.t1.t1, tTuple.t1.t2, tTuple.t1.t3, tTuple.t2) }
                            .doOnNext { fTuple -> fTuple.t1.matches = fTuple.t3.currentMatch }
                            .doOnNext { fTuple ->
                                val calcPos = fTuple.t4
                                fTuple.t1.percentage = (fTuple.t1.total.toDouble() / calcPos.toDouble()).toBigDecimal(percMathContext).toDouble()
                            }
                            .map { fTuple -> Tuples.of(fTuple.t1, true) }
                            .switchIfEmpty(Flux.just(Tuples.of(points, false)))
//                    latestSuccessfulAtt.flatMap { lsa ->
//                        if (lsa!!.parseHash != hasher.hash().asInt()) {
//
//                            Mono.just(points)
//                                    .zipWith(tournamentMono)
//                                    .doOnNext { tTuple -> tTuple.t1.matches = tTuple.t2.currentMatch }
//                                    .flatMap { tTuple ->
//                                        calculatePossible.doOnNext { possiblePts ->
//                                            tTuple.t1.percentage = (tTuple.t1.total / possiblePts).toDouble()
//                                        }
//                                                .map { _ -> tTuple }
//                                    }
//                                    .map { tTuple -> tTuple.t1 }
//                                    .flatMap { pts -> pointsRepository.insert(pts) }
//                                    .map { pts -> Tuples.of(pts, true) }
//                                    .flux()
//                        } else {
//                            Flux.just(Tuples.of(points, false))
//                        }
//                    }
                }
                .filter { pTuple -> pTuple.t2 }
                .map { pTuple ->
                    pTuple.t1
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