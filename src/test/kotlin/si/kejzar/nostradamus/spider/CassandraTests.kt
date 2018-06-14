package si.kejzar.nostradamus.spider

import org.junit.After
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit4.SpringRunner

/**
 * @author matijak
 * @since 15/06/2018
 */
@RunWith(SpringRunner::class)
@SpringBootTest
@ActiveProfiles("test")
abstract class CassandraTests {

    @Autowired
    protected lateinit var userRepository: UserRepository

    @After
    fun cleanUp() {
        userRepository.deleteAll().block()
    }
}