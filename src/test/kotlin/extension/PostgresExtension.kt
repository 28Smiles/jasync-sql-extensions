package extension


import com.github.jasync.sql.db.Connection
import com.github.jasync.sql.db.ConnectionPoolConfiguration
import com.github.jasync.sql.db.postgresql.PostgreSQLConnection
import com.github.jasync.sql.db.postgresql.PostgreSQLConnectionBuilder
import io.github.cdimascio.dotenv.Dotenv
import org.junit.jupiter.api.extension.AfterAllCallback
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.ParameterContext
import org.junit.jupiter.api.extension.ParameterResolver
import org.postgresql.ds.PGSimpleDataSource
import ru.yandex.qatools.embed.postgresql.EmbeddedPostgres
import ru.yandex.qatools.embed.postgresql.distribution.Version
import java.sql.SQLException
import javax.sql.DataSource

class PostgresExtension : BeforeEachCallback, BeforeAllCallback, AfterAllCallback, ParameterResolver {
    @Throws(SQLException::class)
    override fun beforeEach(context: ExtensionContext) {
        val store = context.getStore(ExtensionContext.Namespace.GLOBAL)
        val dataSource = store["dataSource"] as PGSimpleDataSource?
        dataSource?.let { ds -> dropAll(ds, ds.user) }
    }

    override fun afterAll(context: ExtensionContext) {
        val store = context.getStore(ExtensionContext.Namespace.GLOBAL)
        val postgres = store["postgres"] as EmbeddedPostgres?
        postgres?.stop()
        val postgreSQLConnection = store["connection"] as Connection?
        postgreSQLConnection?.disconnect()?.get()
    }

    override fun beforeAll(context: ExtensionContext) {
        val store = context.getStore(ExtensionContext.Namespace.GLOBAL)

        if (!isDedicated()) {
            println("Did not find a dedicated Database, using embedded postgres...")
            store.put("postgres", startupPostgres())
        }

        val configuration = configuration()
        val dataSource = PGSimpleDataSource()
        dataSource.serverName = configuration.host
        dataSource.portNumber = configuration.port
        dataSource.databaseName = configuration.database
        dataSource.user = configuration.username
        dataSource.password = configuration.password
        store.put("dataSource", dataSource)

        store.put("connection", PostgreSQLConnectionBuilder.createConnectionPool(configuration))
    }

    override fun supportsParameter(parameterContext: ParameterContext?, extensionContext: ExtensionContext?): Boolean
            = parameterContext?.parameter?.type?.equals(Connection::class.java) ?: false

    override fun resolveParameter(parameterContext: ParameterContext?, extensionContext: ExtensionContext?): Any
            = extensionContext!!.getStore(ExtensionContext.Namespace.GLOBAL).get("connection")!!

    companion object {
        private val dotenv: Dotenv = Dotenv.configure().ignoreIfMissing().load()
        private val postgresPort = 5422

        private fun isDedicated(): Boolean = dotenv["DEDICATED_TEST_DATABASE_HOST"] != null

        private fun startupPostgres(): EmbeddedPostgres {
            val postgres = EmbeddedPostgres(Version.V10_6)
            postgres.start(EmbeddedPostgres.DEFAULT_HOST, 5422, EmbeddedPostgres.DEFAULT_DB_NAME)

            return postgres
        }

        private fun configuration(): ConnectionPoolConfiguration =
                dotenv["DEDICATED_TEST_DATABASE_HOST"]?.let { databaseHost ->
                    ConnectionPoolConfiguration(
                            host = databaseHost,
                            port = dotenv["DEDICATED_TEST_DATABASE_PORT"]?.toInt()
                                    ?: error("Missing dedicated database port"),
                            database = dotenv["DEDICATED_TEST_DATABASE_NAME"]
                                    ?: error("Missing dedicated database name"),
                            username = dotenv["DEDICATED_TEST_DATABASE_USER"]
                                    ?: error("Missing dedicated database user"),
                            password = dotenv["DEDICATED_TEST_DATABASE_PASSWORD"]
                                    ?: error("Missing dedicated database password")
                    )
                } ?: ConnectionPoolConfiguration(
                        host = EmbeddedPostgres.DEFAULT_HOST,
                        port = postgresPort,
                        database = EmbeddedPostgres.DEFAULT_DB_NAME,
                        username = EmbeddedPostgres.DEFAULT_USER,
                        password = EmbeddedPostgres.DEFAULT_PASSWORD
                )
    }

    @Throws(SQLException::class)
    private fun dropAll(dataSource: DataSource, user: String) {
        dataSource.connection.use { connection ->
            connection.createStatement().use { statement ->
                statement.execute(
                        """
                                DROP SCHEMA public CASCADE;
                                CREATE SCHEMA public;
                                GRANT ALL ON SCHEMA public TO $user;
                                GRANT ALL ON SCHEMA public TO public;
                                """
                )
            }
        }
    }
}
