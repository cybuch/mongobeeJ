package com.github.mongobeej;

import com.github.mongobeej.changeset.ChangeEntry;
import com.github.mongobeej.dao.ChangeEntryDao;
import com.github.mongobeej.exception.MongobeeChangeSetException;
import com.github.mongobeej.exception.MongobeeConfigurationException;
import com.github.mongobeej.exception.MongobeeConnectionException;
import com.github.mongobeej.exception.MongobeeException;
import com.github.mongobeej.utils.ChangeService;
import com.mongodb.*;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import org.jongo.Jongo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.env.Environment;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

import static com.mongodb.ServerAddress.defaultHost;
import static com.mongodb.ServerAddress.defaultPort;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.springframework.util.StringUtils.hasText;

/**
 * Mongobee runner
 *
 * @author lstolowski
 * @since 26/07/2014
 */
public class Mongobee implements InitializingBean {
    private static final Logger logger = LoggerFactory.getLogger(Mongobee.class);

    private static final String DEFAULT_CHANGELOG_COLLECTION_NAME = "dbchangelog";
    private static final String DEFAULT_LOCK_COLLECTION_NAME = "mongobeelock";
    private static final boolean DEFAULT_WAIT_FOR_LOCK = false;
    private static final long DEFAULT_CHANGE_LOG_LOCK_WAIT_TIME = 5L;
    private static final long DEFAULT_CHANGE_LOG_LOCK_POLL_RATE = 10L;
    private static final boolean DEFAULT_THROW_EXCEPTION_IF_CANNOT_OBTAIN_LOCK = false;

    private ChangeEntryDao dao;

    private boolean enabled = true;
    private String changeLogsScanPackage;
    private MongoClientURI mongoClientURI;
    private MongoClient legacyMongoClient;
    private com.mongodb.client.MongoClient mongoClient;
    private String dbName;
    private Environment springEnvironment;

    private MongoTemplate mongoTemplate;
    private Jongo jongo;


    /**
     * <p>Simple constructor with default configuration of host (localhost) and port (27017). Although
     * <b>the database name need to be provided</b> using {@link Mongobee#setDbName(String)} setter.</p>
     * <p>It is recommended to use constructors with MongoURI</p>
     */
    public Mongobee() {
        this(new MongoClientURI("mongodb://" + defaultHost() + ":" + defaultPort() + "/"));
    }

    /**
     * <p>Constructor takes db.mongodb.MongoClientURI object as a parameter.
     * </p><p>For more details about MongoClientURI please see com.mongodb.MongoClientURI docs
     * </p>
     *
     * @param mongoClientURI uri to your db
     * @see MongoClientURI
     */
    public Mongobee(MongoClientURI mongoClientURI) {
        this.mongoClientURI = mongoClientURI;
        this.setDbName(mongoClientURI.getDatabase());
        this.dao = new ChangeEntryDao(DEFAULT_CHANGELOG_COLLECTION_NAME, DEFAULT_LOCK_COLLECTION_NAME, DEFAULT_WAIT_FOR_LOCK,
                DEFAULT_CHANGE_LOG_LOCK_WAIT_TIME, DEFAULT_CHANGE_LOG_LOCK_POLL_RATE, DEFAULT_THROW_EXCEPTION_IF_CANNOT_OBTAIN_LOCK);
    }

    /**
     * <p>Constructor takes db.mongodb.MongoClient object as a parameter.
     * </p><p>For more details about <tt>MongoClient</tt> please see com.mongodb.MongoClient docs
     * </p>
     *
     * @param legacyMongoClient database connection client
     * @see MongoClient
     */
    public Mongobee(MongoClient legacyMongoClient) {
        this.legacyMongoClient = legacyMongoClient;
        this.dao = new ChangeEntryDao(DEFAULT_CHANGELOG_COLLECTION_NAME, DEFAULT_LOCK_COLLECTION_NAME, DEFAULT_WAIT_FOR_LOCK,
                DEFAULT_CHANGE_LOG_LOCK_WAIT_TIME, DEFAULT_CHANGE_LOG_LOCK_POLL_RATE, DEFAULT_THROW_EXCEPTION_IF_CANNOT_OBTAIN_LOCK);
    }

    /**
     * <p>Mongobee runner. Correct MongoDB URI should be provided.</p>
     * <p>The format of the URI is:
     * <pre>
     *   mongodb://[username:password@]host1[:port1][,host2[:port2],...[,hostN[:portN]]][/[database[.collection]][?options]]
     * </pre>
     * <ul>
     * <li>{@code mongodb://} Required prefix</li>
     * <li>{@code username:password@} are optional.  If given, the driver will attempt to login to a database after
     * connecting to a database server. For some authentication mechanisms, only the username is specified and the password is not,
     * in which case the ":" after the username is left off as well.</li>
     * <li>{@code host1} Required.  It identifies a server address to connect to. More than one host can be provided.</li>
     * <li>{@code :portX} is optional and defaults to :27017 if not provided.</li>
     * <li>{@code /database} the name of the database to login to and thus is only relevant if the
     * {@code username:password@} syntax is used. If not specified the "admin" database will be used by default.
     * <b>Mongobee will operate on the database provided here or on the database overridden by setter setDbName(String).</b>
     * </li>
     * <li>{@code ?options} are connection options. For list of options please see com.mongodb.MongoClientURI docs</li>
     * </ul>
     * <p>For details, please see com.mongodb.MongoClientURI
     *
     * @param mongoURI with correct format
     * @see com.mongodb.MongoClientURI
     */
    public Mongobee(String mongoURI) {
        this(new MongoClientURI(mongoURI));
    }

    /**
     * <p>Constructor takes db.mongodb.client.MongoClient object as a parameter.
     * </p><p>For more details about <tt>MongoClient</tt> please see com.mongodb.client.MongoClient docs
     * </p>
     *
     * @param mongoClient database connection client
     * @see MongoClient
     */
    public Mongobee(com.mongodb.client.MongoClient mongoClient) {
        this.mongoClient = mongoClient;
        this.dao = new ChangeEntryDao(DEFAULT_CHANGELOG_COLLECTION_NAME, DEFAULT_LOCK_COLLECTION_NAME, DEFAULT_WAIT_FOR_LOCK,
                DEFAULT_CHANGE_LOG_LOCK_WAIT_TIME, DEFAULT_CHANGE_LOG_LOCK_POLL_RATE, DEFAULT_THROW_EXCEPTION_IF_CANNOT_OBTAIN_LOCK);
    }

    /**
     * For Spring users: executing mongobeej after bean is created in the Spring context
     *
     * @throws Exception exception
     */
    @Override
    public void afterPropertiesSet() throws Exception {
        execute();
    }

    /**
     * Executing migration
     *
     * @throws MongobeeException exception
     */
    public void execute() throws MongobeeException {
        if (!isEnabled()) {
            logger.info("Mongobee is disabled. Exiting.");
            return;
        }

        validateConfig();

        if (this.legacyMongoClient != null) {
            dao.connectMongoDb(this.legacyMongoClient, dbName);
        } else {
            dao.connectMongoDb(this.mongoClientURI, dbName);
        }

        if (!dao.acquireProcessLock()) {
            logger.info("Mongobee did not acquire process lock. Exiting.");
            return;
        }

        logger.info("Mongobee acquired process lock, starting the data migration sequence..");

        try {
            executeMigration();
        } finally {
            logger.info("Mongobee is releasing process lock.");
            dao.releaseProcessLock();
        }

        logger.info("Mongobee has finished his job.");
    }

    private void executeMigration() throws MongobeeConnectionException, MongobeeException {

        ChangeService service = new ChangeService(changeLogsScanPackage, springEnvironment);

        for (Class<?> changelogClass : service.fetchChangeLogs()) {

            Object changelogInstance = null;
            try {
                changelogInstance = changelogClass.getConstructor().newInstance();
                List<Method> changesetMethods = service.fetchChangeSets(changelogInstance.getClass());

                for (Method changesetMethod : changesetMethods) {
                    ChangeEntry changeEntry = service.createChangeEntry(changesetMethod);

                    try {
                        if (dao.isNewChange(changeEntry)) {
                            executeChangeSetMethod(changesetMethod, changelogInstance, dao.getDb(), dao.getMongoDatabase());
                            dao.save(changeEntry);
                            logger.info(changeEntry + " applied");
                        } else if (service.isRunAlwaysChangeSet(changesetMethod)) {
                            executeChangeSetMethod(changesetMethod, changelogInstance, dao.getDb(), dao.getMongoDatabase());
                            logger.info(changeEntry + " reapplied");
                        } else {
                            logger.info(changeEntry + " passed over");
                        }
                    } catch (MongobeeChangeSetException e) {
                        logger.error(e.getMessage());
                    }
                }
            } catch (NoSuchMethodException | IllegalAccessException | InstantiationException exception) {
                throw new MongobeeException(exception.getMessage(), exception);
            } catch (InvocationTargetException exception) {
                Throwable targetException = exception.getTargetException();
                throw new MongobeeException(targetException.getMessage(), exception);
            }
        }
    }

    private Object executeChangeSetMethod(
            Method changeSetMethod,
            Object changeLogInstance,
            DB db,
            MongoDatabase mongoDatabase
    ) throws IllegalAccessException, InvocationTargetException, MongobeeChangeSetException {
        if (changeSetMethod.getParameterTypes().length == 1
                && changeSetMethod.getParameterTypes()[0].equals(DB.class)) {
            logger.debug("method with DB argument");

            return changeSetMethod.invoke(changeLogInstance, db);
        } else if (changeSetMethod.getParameterTypes().length == 1
                && changeSetMethod.getParameterTypes()[0].equals(Jongo.class)) {
            logger.debug("method with Jongo argument");

            return changeSetMethod.invoke(changeLogInstance, jongo != null ? jongo : new Jongo(db));
        } else if (changeSetMethod.getParameterTypes().length == 1
                && changeSetMethod.getParameterTypes()[0].equals(MongoTemplate.class)) {
            logger.debug("method with MongoTemplate argument");

            return changeSetMethod.invoke(changeLogInstance, mongoTemplate != null ? mongoTemplate : createMongoTemplate());
        } else if (changeSetMethod.getParameterTypes().length == 2
                && changeSetMethod.getParameterTypes()[0].equals(MongoTemplate.class)
                && changeSetMethod.getParameterTypes()[1].equals(Environment.class)) {
            logger.debug("method with MongoTemplate and environment arguments");

            return changeSetMethod.invoke(changeLogInstance, mongoTemplate != null ? mongoTemplate : createMongoTemplate(), springEnvironment);
        } else if (changeSetMethod.getParameterTypes().length == 1
                && changeSetMethod.getParameterTypes()[0].equals(MongoDatabase.class)) {
            logger.debug("method with DB argument");

            return changeSetMethod.invoke(changeLogInstance, mongoDatabase);
        } else if (changeSetMethod.getParameterTypes().length == 0) {
            logger.debug("method with no params");

            return changeSetMethod.invoke(changeLogInstance);
        } else {
            throw new MongobeeChangeSetException("ChangeSet method " + changeSetMethod.getName() +
                    " has wrong arguments list. Please see docs for more info!");
        }
    }

    private MongoTemplate createMongoTemplate() {
        com.mongodb.client.MongoClient mongoClient;
        if (this.mongoClient != null) {
            mongoClient = this.mongoClient;
        } else if (legacyMongoClient!= null) {
            mongoClient = fromLegacyClient();
        } else {
            mongoClient = MongoClients.create(mongoClientURI.toString());
        }
        return new MongoTemplate(mongoClient, dbName);
    }

    private com.mongodb.client.MongoClient fromLegacyClient() {
        MongoClientOptions mongoClientOptions = legacyMongoClient.getMongoClientOptions();
        MongoClientSettings.Builder mongoClientSettingsBuilder = MongoClientSettings.builder()
                .readPreference(legacyMongoClient.getReadPreference())
                .writeConcern(legacyMongoClient.getWriteConcern())
                .retryWrites(mongoClientOptions.getRetryWrites())
                .readConcern(legacyMongoClient.getReadConcern())
                .codecRegistry(mongoClientOptions.getCodecRegistry())
                .compressorList(mongoClientOptions.getCompressorList())
                .applicationName(mongoClientOptions.getApplicationName())
                .commandListenerList(mongoClientOptions.getCommandListeners())
                .codecRegistry(mongoClientOptions.getCodecRegistry())
                .applyToClusterSettings(clusterSettings -> {
                    clusterSettings.hosts(legacyMongoClient.getAllAddress());
                    clusterSettings.localThreshold(mongoClientOptions.getLocalThreshold(), MILLISECONDS);
                    clusterSettings.serverSelectionTimeout(mongoClientOptions.getServerSelectionTimeout(), MILLISECONDS);
                    clusterSettings.serverSelector(mongoClientOptions.getServerSelector());
                    clusterSettings.requiredReplicaSetName(mongoClientOptions.getRequiredReplicaSetName());
                    mongoClientOptions.getClusterListeners().forEach(clusterSettings::addClusterListener);
                })
                .applyToConnectionPoolSettings(connectionPoolSettings -> {
                    mongoClientOptions.getConnectionPoolListeners().forEach(connectionPoolSettings::addConnectionPoolListener);
                    connectionPoolSettings.maxConnectionIdleTime(mongoClientOptions.getMaxConnectionIdleTime(), MILLISECONDS);
                    connectionPoolSettings.maxConnectionLifeTime(mongoClientOptions.getMaxConnectionLifeTime(), MILLISECONDS);
                    connectionPoolSettings.maxWaitTime(mongoClientOptions.getMaxWaitTime(), MILLISECONDS);
                })
                .applyToSocketSettings(socketSettings -> {
                    socketSettings.connectTimeout(mongoClientOptions.getConnectTimeout(), MILLISECONDS);
                    socketSettings.readTimeout(mongoClientOptions.getSocketTimeout(), MILLISECONDS);
                })
                .applyToServerSettings(serverSettings -> {
                    mongoClientOptions.getServerListeners().forEach(serverSettings::addServerListener);
                    serverSettings.minHeartbeatFrequency(mongoClientOptions.getMinHeartbeatFrequency(), MILLISECONDS);
                    serverSettings.heartbeatFrequency(mongoClientOptions.getHeartbeatFrequency(), MILLISECONDS);

                })
                .applyToSslSettings(sslSettings -> {
                    sslSettings.enabled(mongoClientOptions.isSslEnabled());
                    sslSettings.invalidHostNameAllowed(mongoClientOptions.isSslInvalidHostNameAllowed());
                    sslSettings.context(mongoClientOptions.getSslContext());
                });
        if (legacyMongoClient.getCredential() != null) {
            mongoClientSettingsBuilder.credential(legacyMongoClient.getCredential());
        }
        return MongoClients.create(mongoClientSettingsBuilder.build());
    }

    private void validateConfig() throws MongobeeConfigurationException {
        if (!hasText(dbName)) {
            throw new MongobeeConfigurationException("DB name is not set. It should be defined in MongoDB URI or via setter");
        }
        if (!hasText(changeLogsScanPackage)) {
            throw new MongobeeConfigurationException("Scan package for changelogs is not set: use appropriate setter");
        }
    }

    /**
     * @return true if an execution is in progress, in any process.
     * @throws MongobeeConnectionException exception
     */
    public boolean isExecutionInProgress() throws MongobeeConnectionException {
        return dao.isProccessLockHeld();
    }

    /**
     * Used DB name should be set here or via MongoDB URI (in a constructor)
     *
     * @param dbName database name
     * @return Mongobee object for fluent interface
     */
    public Mongobee setDbName(String dbName) {
        this.dbName = dbName;
        return this;
    }

    /**
     * Sets uri to MongoDB
     *
     * @param mongoClientURI object with defined mongo uri
     * @return Mongobee object for fluent interface
     */
    public Mongobee setMongoClientURI(MongoClientURI mongoClientURI) {
        this.mongoClientURI = mongoClientURI;
        return this;
    }

    /**
     * Package name where @ChangeLog-annotated classes are kept.
     *
     * @param changeLogsScanPackage package where your changelogs are
     * @return Mongobee object for fluent interface
     */
    public Mongobee setChangeLogsScanPackage(String changeLogsScanPackage) {
        this.changeLogsScanPackage = changeLogsScanPackage;
        return this;
    }

    /**
     * @return true if Mongobee runner is enabled and able to run, otherwise false
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Feature which enables/disables Mongobee runner execution
     *
     * @param enabled MOngobee will run only if this option is set to true
     * @return Mongobee object for fluent interface
     */
    public Mongobee setEnabled(boolean enabled) {
        this.enabled = enabled;
        return this;
    }

    /**
     * Feature which enables/disables waiting for lock if it's already obtained
     *
     * @param waitForLock Mongobee will be waiting for lock if it's already obtained if this option is set to true
     * @return Mongobee object for fluent interface
     */
    public Mongobee setWaitForLock(boolean waitForLock) {
        this.dao.setWaitForLock(waitForLock);
        return this;
    }

    /**
     * Waiting time for acquiring lock if waitForLock is true
     *
     * @param changeLogLockWaitTime Waiting time in minutes for acquiring lock
     * @return Mongobee object for fluent interface
     */
    public Mongobee setChangeLogLockWaitTime(long changeLogLockWaitTime) {
        this.dao.setChangeLogLockWaitTime(changeLogLockWaitTime);
        return this;
    }

    /**
     * Poll rate for acquiring lock if waitForLock is true
     *
     * @param changeLogLockPollRate Poll rate in seconds for acquiring lock
     * @return Mongobee object for fluent interface
     */
    public Mongobee setChangeLogLockPollRate(long changeLogLockPollRate) {
        this.dao.setChangeLogLockPollRate(changeLogLockPollRate);
        return this;
    }

    /**
     * Feature which enables/disables throwing MongobeeLockException if Mongobee can not obtain lock
     *
     * @param throwExceptionIfCannotObtainLock Mongobee will throw MongobeeLockException if lock can not be obtained
     * @return Mongobee object for fluent interface
     */
    public Mongobee setThrowExceptionIfCannotObtainLock(boolean throwExceptionIfCannotObtainLock) {
        this.dao.setThrowExceptionIfCannotObtainLock(throwExceptionIfCannotObtainLock);
        return this;
    }

    /**
     * Set Environment object for Spring Profiles (@Profile) integration
     *
     * @param environment org.springframework.core.env.Environment object to inject
     * @return Mongobee object for fluent interface
     */
    public Mongobee setSpringEnvironment(Environment environment) {
        this.springEnvironment = environment;
        return this;
    }

    /**
     * Sets pre-configured {@link MongoTemplate} instance to use by the Mongobee
     *
     * @param mongoTemplate instance of the {@link MongoTemplate}
     * @return Mongobee object for fluent interface
     */
    public Mongobee setMongoTemplate(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
        return this;
    }

    /**
     * Sets pre-configured {@link MongoTemplate} instance to use by the Mongobee
     *
     * @param jongo {@link Jongo} instance
     * @return Mongobee object for fluent interface
     */
    public Mongobee setJongo(Jongo jongo) {
        this.jongo = jongo;
        return this;
    }

    /**
     * Overwrites a default mongobeej changelog collection hardcoded in DEFAULT_CHANGELOG_COLLECTION_NAME.
     * <p>
     * CAUTION! Use this method carefully - when changing the name on a existing system,
     * your changelogs will be executed again on your MongoDB instance
     *
     * @param changelogCollectionName a new changelog collection name
     * @return Mongobee object for fluent interface
     */
    public Mongobee setChangelogCollectionName(String changelogCollectionName) {
        this.dao.setChangelogCollectionName(changelogCollectionName);
        return this;
    }

    /**
     * Overwrites a default mongobeej lock collection hardcoded in DEFAULT_LOCK_COLLECTION_NAME
     *
     * @param lockCollectionName a new lock collection name
     * @return Mongobee object for fluent interface
     */
    public Mongobee setLockCollectionName(String lockCollectionName) {
        this.dao.setLockCollectionName(lockCollectionName);
        return this;
    }

    /**
     * Closes the Mongo instance used by Mongobee.
     * This will close either the connection Mongobee was initiated with or that which was internally created.
     */
    public void close() {
        dao.close();
    }
}
