package si.kejzar.nostradamus.spider

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.CommandLineRunner
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux
import javax.annotation.PostConstruct

/**
 * @author matijak
 * @since 20/06/2018
 */
@Component
class UserImporter  @Autowired constructor(private val tournamentRepository: TournamentRepository)    {

    @PostConstruct
    fun importUsersIfNecessary() {

        getTournaments()
                .subscribe({
                    t : Tournament ->
                    println("t = ${t}")
                })

    }

    private fun getTournaments() : Flux<Tournament> {

        return tournamentRepository.findByUsersImported(false)

    }

}