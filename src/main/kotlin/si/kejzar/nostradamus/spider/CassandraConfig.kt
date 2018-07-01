package si.kejzar.nostradamus.spider

import com.datastax.driver.core.Cluster
import com.datastax.driver.extras.codecs.jdk8.ZonedDateTimeCodec
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.cassandra.ClusterBuilderCustomizer
import org.springframework.context.annotation.Configuration
import com.datastax.driver.core.TupleType
import com.datastax.driver.extras.codecs.jdk8.InstantCodec


/**
 * @author matijak
 * @since 01/07/2018
 */
@Configuration
class CassandraConfig : ClusterBuilderCustomizer {

    override fun customize(clusterBuilder: Cluster.Builder?) {
//        val tupleType = clusterBuildergetMetadata()
//                .newTupleType(DataType.timestamp(), DataType.varchar())
//        clusterBuilder!!.configuration.codecRegistry.register(ZonedDateTimeCodec.     )

        clusterBuilder!!.configuration.codecRegistry.register(InstantCodec.instance)
    }
}