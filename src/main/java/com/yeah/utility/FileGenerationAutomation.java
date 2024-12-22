package com.yeah.utility;

import java.io.IOException;
import java.util.List;

public class FileGenerationAutomation {

    public static void main(String[] args) throws IOException, InterruptedException {
        ConfigLoader config = ConfigLoader.getInstance();

        String schemaFilePath = config.getProperty("schema.file.path");
        String generatedDataLocation = config.getProperty("generated.data.location");
        int fileRecordsCount = Integer.parseInt(config.getProperty("file.records.count"));
        int fileGenerationInterval = Integer.parseInt(config.getProperty("file.generation.interval.seconds"));
        int fileBatchSize = Integer.parseInt(config.getProperty("file.generation.batch.size"));
        int pauseTime = Integer.parseInt(config.getProperty("file.generation.pause.seconds"));

        List<String> frequentUsers = config.getListProperty("frequent.users");
        List<String> frequentLocations = config.getListProperty("frequent.locations");

        SyntheticTwitterDataGenerator generator = new SyntheticTwitterDataGenerator(schemaFilePath, frequentUsers, frequentLocations);

        int fileCount = 0;
        while (true) {
            for (int i = 0; i < fileBatchSize; i++) {
                String outputPath = String.format("%s/file_%d.avro", generatedDataLocation, ++fileCount);
                generator.generateDataFile(outputPath, fileRecordsCount);
                System.out.println("Generated: " + outputPath);
                Thread.sleep(fileGenerationInterval * 1000);
            }
            System.out.println("Pausing for " + pauseTime + " seconds...");
            Thread.sleep(pauseTime * 1000);
        }
    }
}