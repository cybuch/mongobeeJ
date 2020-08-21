package com.github.mongobeej.utils;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import de.bwaldvogel.mongo.MongoServer;
import de.bwaldvogel.mongo.backend.memory.MemoryBackend;

import java.net.InetSocketAddress;

import static java.util.Objects.requireNonNull;

public class MongoEnvironmentCreator {
    public static final String DB_NAME = "mongobeetest";

    public static class MongoEnvironment {
        private final MongoClient mongoClient;
        private final MongoDatabase mongoDatabase;

        MongoEnvironment(MongoClient mongoClient) {
            this.mongoClient = requireNonNull(mongoClient);
            mongoDatabase = mongoClient.getDatabase(DB_NAME);
        }

        public MongoClient getMongoClient() {
            return mongoClient;
        }

        public MongoDatabase getMongoDatabase() {
            return mongoDatabase;
        }
    }

    public static MongoEnvironment createMongoEnvironment() {
        MongoServer mongoServer = new MongoServer(new MemoryBackend());
        InetSocketAddress serverAddress = mongoServer.bind();
        String mongoUri = "mongodb:/" + serverAddress.getAddress().toString() + ":" + serverAddress.getPort() + "/" + DB_NAME;
        MongoClient mongoClient = MongoClients.create(mongoUri);
        return new MongoEnvironment(mongoClient);
    }
}
