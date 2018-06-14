package si.kejzar.nostradamus.spider

import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Profile
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit4.SpringRunner
import kotlin.test.assertEquals
import kotlin.test.assertNotNull


class UserRepositoryTests : CassandraTests() {



	@Test
	fun saveUser() {
		val user = User(name = "Matija", userName = "mm04", emailAddress = "madmatt04@gmail.com")
        assertEquals("Matija", user.name)
        assertEquals("mm04", user.userName)
        assertEquals("madmatt04@gmail.com", user.emailAddress)
        assertNotNull(user.id);

        val savedUser = userRepository.insert(user).block()
        assertNotNull(savedUser)
        assertEquals(user, savedUser)
	}

}
