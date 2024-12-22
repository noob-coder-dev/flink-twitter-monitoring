package com.yeah.utility;

import com.github.javafaker.Faker;
import org.apache.avro.Schema;
import org.apache.avro.file.DataFileWriter;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericDatumWriter;
import org.apache.avro.io.DatumWriter;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class SyntheticTwitterDataGenerator {
    private final Schema schema;
    private final Faker faker;
    private final Random random;
    private final List<String> frequentUsers;
    private final List<String> frequentLocations;

    public SyntheticTwitterDataGenerator(String schemaFilePath, List<String> frequentUsers, List<String> frequentLocations) throws IOException {
        this.schema = new Schema.Parser().parse(new File(schemaFilePath));
        this.faker = new Faker();
        this.random = new Random();
        this.frequentUsers = frequentUsers;
        this.frequentLocations = frequentLocations;
    }

    public void generateDataFile(String outputPath, int recordCount) throws IOException {
        File avroFile = new File(outputPath);
        DatumWriter<GenericData.Record> datumWriter = new GenericDatumWriter<>(schema);
        DataFileWriter<GenericData.Record> dataFileWriter = new DataFileWriter<>(datumWriter);
        dataFileWriter.create(schema, avroFile);

        for (int i = 0; i < recordCount; i++) {
            GenericData.Record record = createRecord();
            dataFileWriter.append(record);
        }

        dataFileWriter.close();
    }

    private GenericData.Record createRecord() {
        GenericData.Record record = new GenericData.Record(schema);

        record.put("tweet_id", faker.internet().uuid());
        record.put("username", random.nextInt(10) < 7 ? getRandomFrequentUser() : faker.name().username());
        record.put("display_name", random.nextBoolean() ? faker.name().fullName() : null);
        record.put("tweet", faker.lorem().sentence(15));
        record.put("timestamp", Instant.now().toEpochMilli());
        record.put("geo_location", random.nextInt(10) < 7 ? createFrequentGeoLocationRecord() : createGeoLocationRecord());
        record.put("hashtags", generateHashtags());
        record.put("mentions", generateMentions());
        record.put("likes", random.nextInt(10000));
        record.put("retweets", random.nextInt(5000));
        record.put("is_retweet", random.nextBoolean());
        record.put("media_attachments", random.nextBoolean() ? generateMediaAttachments() : null);
        record.put("reply_to_tweet_id", random.nextBoolean() ? faker.internet().uuid() : null);
        record.put("user_metadata", createUserMetadataRecord());

        return record;
    }

    private GenericData.Record createGeoLocationRecord() {
        Schema geoLocationSchema = schema.getField("geo_location").schema().getTypes().get(1);
        GenericData.Record geoLocation = new GenericData.Record(geoLocationSchema);
        geoLocation.put("latitude", random.nextDouble() * 180 - 90);
        geoLocation.put("longitude", random.nextDouble() * 360 - 180);
        return geoLocation;
    }

    private GenericData.Record createFrequentGeoLocationRecord() {
        Schema geoLocationSchema = schema.getField("geo_location").schema().getTypes().get(1);
        GenericData.Record geoLocation = new GenericData.Record(geoLocationSchema);
        String location = frequentLocations.get(random.nextInt(frequentLocations.size()));
        geoLocation.put("latitude", location.equals("NewYork") ? 40.7128 : (location.equals("LosAngeles") ? 34.0522 : 12.9716));
        geoLocation.put("longitude", location.equals("NewYork") ? -74.0060 : (location.equals("LosAngeles") ? -118.2437 : 77.5946));
        return geoLocation;
    }

    private List<String> generateHashtags() {
        List<String> hashtags = new ArrayList<>();
        int count = random.nextInt(5);
        for (int i = 0; i < count; i++) {
            hashtags.add("#" + faker.lorem().word());
        }
        return hashtags;
    }

    private List<String> generateMentions() {
        List<String> mentions = new ArrayList<>();
        int count = random.nextInt(3);
        for (int i = 0; i < count; i++) {
            mentions.add("@" + faker.name().username());
        }
        return mentions;
    }

    private List<GenericData.Record> generateMediaAttachments() {
        Schema mediaSchema = schema.getField("media_attachments").schema().getTypes().get(1).getElementType();
        List<GenericData.Record> mediaAttachments = new ArrayList<>();
        int count = random.nextInt(3);
        for (int i = 0; i < count; i++) {
            GenericData.Record media = new GenericData.Record(mediaSchema);
            media.put("media_type", random.nextBoolean() ? "image" : "video");
            media.put("url", faker.internet().url());
            mediaAttachments.add(media);
        }
        return mediaAttachments;
    }

    private GenericData.Record createUserMetadataRecord() {
        Schema userMetadataSchema = schema.getField("user_metadata").schema();
        GenericData.Record userMetadata = new GenericData.Record(userMetadataSchema);
        userMetadata.put("followers_count", random.nextInt(1000000));
        userMetadata.put("following_count", random.nextInt(5000));
        userMetadata.put("account_created", Instant.now().minusMillis(random.nextInt((int) (10L * 365 * 24 * 60 * 60 * 1000))).toEpochMilli());
        userMetadata.put("verified", random.nextBoolean());
        return userMetadata;
    }

    private String getRandomFrequentUser() {
        return frequentUsers.get(random.nextInt(frequentUsers.size()));
    }
}