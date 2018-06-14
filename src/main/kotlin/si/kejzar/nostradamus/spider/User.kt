package si.kejzar.nostradamus.spider

import org.springframework.data.cassandra.core.mapping.PrimaryKey
import org.springframework.data.cassandra.core.mapping.Table
import java.io.Serializable
import java.util.*

/**
 * @author matijak
 * @since 14/06/2018
 */
@Table("user")
data class User(@PrimaryKey var id: UUID = UUID.randomUUID(), var name: String, var userName: String, var emailAddress: String? = null) : Serializable